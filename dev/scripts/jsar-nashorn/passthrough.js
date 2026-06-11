// Passthrough script: returns the input record unchanged.
// Called by both job-processor2 (Nashorn) and job-processor-graaljs (GraalJS).
// Signature: process(data: String, supplement: Object) → String
function process(data, supplement) {
    return data;
}
