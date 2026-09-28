#!/usr/bin/env python3
"""Upload a data file to the file-store and submit a job referencing it.

Targets the local developer stack described in dev/README.md by default. Use
--instance to submit to staging or production instead, or --file-store and
--job-store to address individual services.

The job-store hands the job to the flow binder that matches the packaging,
format, charset, submitter and destination of the job specification, so all
five need to match a binder registered in the target instance's flow-store.

Uses only the Python standard library.
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

def cluster_urls(environment):
    """Service URLs for a dataIO deployment in the metascrum cluster."""
    return (
        "http://dataio-filestore-service.metascrum-{0}.svc.cloud.dbc.dk"
        "/dataio/file-store-service".format(environment),
        "http://dataio-jobstore-service.metascrum-{0}.svc.cloud.dbc.dk"
        "/dataio/job-store-service".format(environment),
    )


LOCAL_URLS = (
    "http://localhost:8082/dataio/file-store-service",
    "http://localhost:8080/dataio/job-store-service",
)

# Named environments, each as (file-store URL, job-store URL).
INSTANCES = {
    "local": LOCAL_URLS,
    "staging": cluster_urls("staging"),
    "prod": cluster_urls("prod"),
}

LOCAL_HOSTNAMES = {"localhost", "127.0.0.1", "::1", ""}

# Resolves against the flow binders that dev/scripts/seed-flowstore.sh creates.
DEV_SPECIFICATION = {
    "type": "TRANSIENT",
    "packaging": "JSON",
    "format": "test",
    "charset": "utf8",
    "destination": "dev-nashorn",
    "submitterId": 870970,
    "resultmailInitials": "DEV",
    "mailForNotificationAboutProcessing": "",
    "mailForNotificationAboutVerification": "",
}

# The job-store resolves a flow binder from these five, so a job needs all of
# them. See AddJobParam.lookupFlowBinder.
RESOLUTION_FIELDS = ("packaging", "format", "charset", "submitterId", "destination")

REQUIRED_SPECIFICATION_FIELDS = frozenset(RESOLUTION_FIELDS) | {"type"}

DATAFILE_URN_PREFIX = "urn:dataio-fs:"


class Failure(Exception):
    """An error worth reporting to the user without a traceback."""


def http_request(url, method="GET", data=None, headers=None, timeout=60):
    """Perform an HTTP request and return (status, headers, body bytes).

    A 4xx or 5xx response is returned like any other rather than raised, so
    callers can include the response body in their own error message.
    """
    request = urllib.request.Request(url, data=data, method=method)
    for name, value in (headers or {}).items():
        request.add_header(name, value)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, response.headers, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.headers, error.read()
    except urllib.error.URLError as error:
        raise Failure("cannot reach {}: {}".format(url, error.reason)) from error


def decode(body):
    return body.decode("utf-8", errors="replace").strip()


def is_local_url(url):
    return urllib.parse.urlsplit(url).hostname in LOCAL_HOSTNAMES


def resolve_endpoints(instance, timeout):
    """Ask a dataIO instance for the URLs of its services.

    The instance serves a map of service name to base URL at /urls, the same
    endpoint the Java CLI tools use.
    """
    url = instance.rstrip("/") + "/urls"
    status, _, body = http_request(url, timeout=timeout)
    if status != 200:
        raise Failure("{} returned {}: {}".format(url, status, decode(body)))
    try:
        return json.loads(body)
    except json.JSONDecodeError as error:
        raise Failure("{} returned malformed JSON: {}".format(url, decode(body))) from error


def upload_datafile(file_store, path, timeout):
    """POST the file to the file-store and return the new file-store id."""
    url = file_store.rstrip("/") + "/files"
    size = os.path.getsize(path)
    with open(path, "rb") as datafile:
        status, headers, body = http_request(
            url,
            method="POST",
            data=datafile,
            headers={
                "Content-Type": "application/octet-stream",
                "Content-Length": str(size),
            },
            timeout=timeout,
        )
    if status != 201:
        raise Failure("file upload to {} returned {}: {}".format(url, status, decode(body)))
    location = headers.get("Location")
    if not location:
        raise Failure("file upload to {} returned no Location header".format(url))
    return location.rstrip("/").rsplit("/", 1)[-1]


def load_specification(path):
    """Read a job specification from a JSON file."""
    with open(path, encoding="utf-8") as specification_file:
        try:
            specification = json.load(specification_file)
        except json.JSONDecodeError as error:
            raise Failure("{} is not valid JSON: {}".format(path, error)) from error
    if not isinstance(specification, dict):
        raise Failure("{} must contain a JSON object".format(path))
    if "jobSpecification" in specification:
        raise Failure(
            "{} holds a whole job input stream. This script wants the bare "
            "job specification, so pass the contents of its jobSpecification "
            "field instead.".format(path)
        )
    return specification


def specification_overrides(args):
    overrides = {
        "type": args.type,
        "packaging": args.packaging,
        "format": args.format,
        "charset": args.charset,
        "destination": args.destination,
        "submitterId": args.submitter,
        "resultmailInitials": args.result_mail_initials,
    }
    return {k: v for k, v in overrides.items() if v is not None}


def build_specification(args, target_is_local):
    """Assemble the job specification from the base, the file and the flags."""
    overrides = specification_overrides(args)

    if args.spec:
        specification = load_specification(args.spec)
    elif target_is_local:
        specification = dict(DEV_SPECIFICATION)
    elif REQUIRED_SPECIFICATION_FIELDS <= set(overrides):
        specification = {}
    else:
        missing = sorted(REQUIRED_SPECIFICATION_FIELDS - set(overrides))
        raise Failure(
            "the built-in job specification only resolves against the local "
            "developer stack. Submitting elsewhere needs --spec, or every "
            "required field given as a flag. Missing: {}".format(", ".join(missing))
        )

    specification.update(overrides)

    missing = sorted(REQUIRED_SPECIFICATION_FIELDS - set(specification))
    if missing:
        raise Failure("job specification is missing: {}".format(", ".join(missing)))
    return specification


def submit_job(job_store, specification, is_end_of_job, part_number, timeout):
    """POST the job input stream to the job-store and return the job snapshot."""
    url = job_store.rstrip("/") + "/jobs"
    job_input_stream = {
        "jobSpecification": specification,
        "isEndOfJob": is_end_of_job,
        "partNumber": part_number,
    }
    status, _, body = http_request(
        url,
        method="POST",
        data=json.dumps(job_input_stream).encode("utf-8"),
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        timeout=timeout,
    )
    if status != 201:
        raise Failure("job creation at {} returned {}: {}".format(url, status, decode(body)))
    try:
        return json.loads(body)
    except json.JSONDecodeError as error:
        raise Failure("job-store returned malformed JSON: {}".format(decode(body))) from error


def parse_arguments(argv):
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )

    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("datafile", nargs="?", help="data file to upload to the file-store")
    source.add_argument(
        "--datafile-id",
        help="use a file already in the file-store instead of uploading one",
    )

    parser.add_argument(
        "--spec",
        metavar="FILE",
        help="job specification as JSON, as the bare specification rather than "
        "a job input stream wrapping one. Required unless the target is the "
        "local stack, whose built-in specification matches the flow binders "
        "from seed-flowstore.sh.",
    )

    endpoints = parser.add_argument_group("target instance")
    endpoints.add_argument(
        "--instance",
        metavar="URL",
        help="dataIO instance to submit to: local (default), staging, prod, or "
        "the URL of an instance serving /urls. Run with --dry-run to see which "
        "service URLs a name stands for.",
    )
    endpoints.add_argument(
        "--file-store",
        metavar="URL",
        help="file-store base URL, overriding the instance lookup",
    )
    endpoints.add_argument(
        "--job-store",
        metavar="URL",
        help="job-store base URL, overriding the instance lookup",
    )
    endpoints.add_argument(
        "--timeout",
        type=float,
        default=60.0,
        metavar="SECONDS",
        help="per-request timeout (default: %(default)s)",
    )
    endpoints.add_argument(
        "-y",
        "--yes",
        action="store_true",
        help="skip the confirmation prompt shown before submitting to an "
        "instance other than the local stack",
    )

    overrides = parser.add_argument_group("job specification fields")
    overrides.add_argument("--type", help="job type, e.g. TRANSIENT, PERSISTENT, TEST")
    overrides.add_argument("--packaging", help="takes part in flow binder resolution")
    overrides.add_argument("--format", help="takes part in flow binder resolution")
    overrides.add_argument("--charset", help="takes part in flow binder resolution")
    overrides.add_argument(
        "--destination",
        help="destination of the flow binder to resolve, any value registered "
        "in the target flow-store",
    )
    overrides.add_argument(
        "--submitter",
        type=int,
        help="submitter number, takes part in flow binder resolution",
    )
    overrides.add_argument("--result-mail-initials")

    parts = parser.add_argument_group("multi-part jobs")
    parts.add_argument(
        "--part-number",
        type=int,
        default=0,
        help="part number of this submission (default: %(default)s)",
    )
    parts.add_argument(
        "--not-end-of-job",
        action="store_true",
        help="mark the job as awaiting further parts",
    )

    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="print the job input stream that would be posted and stop",
    )

    return parser.parse_args(argv)


def resolve_instance(instance):
    """Split the --instance value into (file-store, job-store, URL to resolve).

    A named environment carries its two service URLs outright. Any other
    instance is a base URL whose /urls endpoint reports them, which is how the
    Java CLI tools find their endpoints.
    """
    if instance is None:
        return INSTANCES["local"] + (None,)
    if instance in INSTANCES:
        return INSTANCES[instance] + (None,)
    if urllib.parse.urlsplit(instance).scheme not in ("http", "https"):
        raise Failure(
            "unknown instance {!r}. Use one of {}, or the URL of an instance "
            "serving /urls.".format(instance, ", ".join(sorted(INSTANCES)))
        )
    return None, None, instance


def target_urls(args):
    """The URLs this run will talk to, as far as they are known without asking.

    An instance that has to be asked for its service URLs contributes its own
    URL, which is enough to tell a local target from a remote one.
    """
    file_store, job_store, resolve_url = resolve_instance(args.instance)
    known = [resolve_url] if resolve_url else []
    known.append(args.file_store or file_store)
    known.append(args.job_store or job_store)
    return [url for url in known if url]


def resolve_service_urls(args):
    """Work out which file-store and job-store to talk to."""
    file_store, job_store, resolve_url = resolve_instance(args.instance)

    if resolve_url and not (args.file_store and args.job_store):
        urls = resolve_endpoints(resolve_url, args.timeout)
        file_store = urls.get("FILESTORE_URL")
        job_store = urls.get("JOBSTORE_URL")
        missing = [
            name
            for name, url in (("FILESTORE_URL", file_store), ("JOBSTORE_URL", job_store))
            if not url
        ]
        if missing:
            raise Failure("{} did not report {}".format(resolve_url, ", ".join(missing)))

    file_store = args.file_store or file_store
    job_store = args.job_store or job_store
    if not (file_store and job_store):
        raise Failure("no file-store and job-store to submit to")
    return file_store, job_store


def report_target(args):
    """Print where a run would go, without asking the network."""
    file_store, job_store, resolve_url = resolve_instance(args.instance)
    file_store = args.file_store or file_store
    job_store = args.job_store or job_store
    lookup = "from {}/urls".format(resolve_url.rstrip("/")) if resolve_url else "unknown"
    print("file-store: {}".format(file_store or lookup), file=sys.stderr)
    print("job-store:  {}".format(job_store or lookup), file=sys.stderr)


def confirm_remote_target(file_store, job_store, skip_prompt):
    """Have the user acknowledge a submission to anything but the local stack."""
    if skip_prompt:
        return
    print("About to submit to a non-local dataIO instance:", file=sys.stderr)
    print("  file-store: {}".format(file_store), file=sys.stderr)
    print("  job-store:  {}".format(job_store), file=sys.stderr)
    if not sys.stdin.isatty():
        raise Failure("refusing to submit without a terminal to confirm at. Pass --yes.")
    if input("Continue? [y/N] ").strip().lower() not in ("y", "yes"):
        raise Failure("aborted")


def main(argv=None):
    args = parse_arguments(argv)

    if args.datafile and not os.path.isfile(args.datafile):
        raise Failure("no such file: {}".format(args.datafile))

    is_local = all(is_local_url(url) for url in target_urls(args))
    specification = build_specification(args, is_local)

    if args.dry_run:
        report_target(args)
        specification["dataFile"] = DATAFILE_URN_PREFIX + str(
            args.datafile_id or "<not uploaded, dry run>"
        )
        print(
            json.dumps(
                {
                    "jobSpecification": specification,
                    "isEndOfJob": not args.not_end_of_job,
                    "partNumber": args.part_number,
                },
                indent=2,
            )
        )
        return 0

    file_store, job_store = resolve_service_urls(args)
    if not is_local:
        confirm_remote_target(file_store, job_store, args.yes)

    if args.datafile_id:
        datafile_id = args.datafile_id
    else:
        print("Uploading {} to {}".format(args.datafile, file_store), file=sys.stderr)
        datafile_id = upload_datafile(file_store, args.datafile, args.timeout)

    specification["dataFile"] = DATAFILE_URN_PREFIX + str(datafile_id)

    print("Creating job at {}".format(job_store), file=sys.stderr)
    snapshot = submit_job(
        job_store,
        specification,
        is_end_of_job=not args.not_end_of_job,
        part_number=args.part_number,
        timeout=args.timeout,
    )

    job_id = snapshot.get("jobId")
    if job_id is None:
        raise Failure("job-store response held no jobId: {}".format(json.dumps(snapshot)))
    print("  data file: {}".format(specification["dataFile"]), file=sys.stderr)
    print(job_id)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Failure as failure:
        print("ERROR: {}".format(failure), file=sys.stderr)
        sys.exit(1)
    except KeyboardInterrupt:
        sys.exit(130)
