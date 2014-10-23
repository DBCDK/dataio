========================
jobstore design dokument
========================

.. |date| date::

:author: Jan Bauer Nielsen <jbn@dbc.dk>
:date: |date|


.. header::

   .. oddeven::

      .. class:: headertable

      +---+---------------------+----------------+
      |   |.. class:: centered  |.. class:: right|
      |   |                     |                |
      |   |Afsnit  ###Section###|Side ###Page### |
      +---+---------------------+----------------+

      .. class:: headertable

      +---------------+---------------------+---+
      |               |.. class:: centered  |   |
      |               |                     |   |
      |Side ###Page###|Afsnit  ###Section###|   |
      +---------------+---------------------+---+


.. contents::

.. section-numbering::

.. raw:: pdf

   PageBreak oneColumn


Formål
======

Formålet med dette dokument er at...


Design spørgsmål
================

Optimistisk VS, pessimistisk låsning?

Delvist dokument opdatering?

RESTful API
===========

.. code-block:: rst

    POST job-store/jobs

.. code-block:: rst

    POST job-store/jobs/{jobId}/chunks/{chunkId}/items/{itemId}/contents/processing
    VS.
    POST job-store/jobs/{jobId}/chunks/{chunkId}/items/processing

.. code-block:: rst

    POST job-store/jobs/{jobId}/chunks/{chunkId}/items/{itemId}/contents/delivering
    VS.
    POST job-store/jobs/{jobId}/chunks/{chunkId}/items/delivering

.. code-block:: rst

    GET job-store/jobs

.. code-block:: rst

    GET job-store/jobs/{jobId}

.. code-block:: rst

    GET job-store/jobs/{jobId}/flow

.. code-block:: rst

    GET job-store/jobs/{jobId}/sink

.. code-block:: rst

    GET job-store/jobs/{jobId}/chunks

.. code-block:: rst

    GET job-store/jobs/{jobId}/chunks/{chunkId}

.. code-block:: rst

    GET job-store/jobs/{jobId}/chunks/{chunkId}/items

.. code-block:: rst

    GET job-store/jobs/{jobId}/chunks/{chunkId}/items/processing

.. code-block:: rst

    GET job-store/jobs/{jobId}/chunks/{chunkId}/items/delivering

.. code-block:: rst

    GET job-store/jobs/{jobId}/chunks/{chunkId}/items/{itemId}

.. code-block:: rst

    GET job-store/jobs/{jobId}/chunks/{chunkId}/items/{itemId}/contents

.. code-block:: rst

    GET job-store/jobs/{jobId}/chunks/{chunkId}/items/{itemId}/contents/chunkifying

.. code-block:: rst

    GET job-store/jobs/{jobId}/chunks/{chunkId}/items/{itemId}/contents/processing

.. code-block:: rst

    GET job-store/jobs/{jobId}/chunks/{chunkId}/items/{itemId}/contents/delivering

.. code-block:: rst

    GET job-store/jobs/{jobId}/items


Appendix
========

.. code-block:: json

    "job": {
        "id": 1234,
        "numberOfChunks": 42,
        "numberOfItems: 427,
        "timeOfCreation": "2014-10-21 T 08:02.001",
        "timeOfCompletion": "",
        "timeOfLastModification": "2014-10-21 T 08:03.989",
        "errorCode": "",
        "jobSpecification": {...}
        "flowName": "flow-1",
        "flow": {...},
        "sinkName": "sink-1",
        "sink": {...},

        "state": {
            "chunkifying": {
                "begin": "2014-10-21 T 08:02.123",
                "end": "2014-10-21 T 08:03.333",
                "pending": 0,
                "active": 0,
                "done": 427,
                "succeeded": 427,
                "failed": 0,
                "ignored": 0
            },
            "processing": {
                "begin": "2014-10-21 T 08:03.343",
                "end": "",
                "pending": 50,
                "active": 70,
                "done": 307,
                "succeeded": 300,
                "failed": 0,
                "ignored": 7
            },
            "delivering": {
                "begin": "2014-10-21 T 08:03.989",
                "end": "",
                "pending": 120,
                "active": 157,
                "done": 150,
                "succeeded": 148,
                "failed": 1,
                "ignored": 1
            }
        }
    }

.. code-block:: json

    "chunk": {
        "id": 1,
        "jobId": 1234,
        "numberOfItems: 10,
        "timeOfCreation": "2014-10-21 T 08:02.001",
        "timeOfCompletion": "",
        "timeOfLastModification": "2014-10-21 T 08:03.989",
        "sequenceAnalysisData": {...},

        "state": {
            "chunkifying": {
                "begin": "2014-10-21 T 08:02.452",
                "end": "2014-10-21 T 08:02.786",
                "pending": 0,
                "active": 0,
                "done": 10,
                "succeeded": 10,
                "failed": 0,
                "ignored": 0
            },
            "processing": {
                "begin": "2014-10-21 T 08:02.800",
                "end": "2014-10-21 T 08:03.000",
                "pending": 0,
                "active": 0,
                "done": 10,
                "succeeded": 9,
                "failed": 0,
                "ignored": 1
            },
            "delivering": {
                "begin": "2014-10-21 T 08:03.013",
                "end": "",
                "pending": 0,
                "active": 10,
                "done": 0,
                "succeeded": 0,
                "failed": 0,
                "ignored": 0
            }
        }
    }

.. code-block:: json

    "item": {
        "id": 1,
        "chunkId": 1,
        "jobId": 1234,
        "timeOfCreation": "2014-10-21 T 08:02.456",
        "timeOfCompletion": "2014-10-21 T 08:02.504",
        "timeOfLastModification": "2014-10-21 T 08:02.504",

        "contents": {
            "chunkifying": {
                "encoding": "utf-8",
                "data": "..."
            },
            "processing": {
                "encoding": "utf-8",
                "data": "..."
            },
            "delivering": {
                "encoding": "utf-8",
                "data": "..."
            }
        }

        "state": {
            "chunkifying": {
                "begin": "2014-10-21 T 08:02.452",
                "end": "2014-10-21 T 08:02.500",
                "pending": 0,
                "active": 0,
                "done": 1,
                "succeeded": 1,
                "failed": 0,
                "ignored": 0
            },
            "processing": {
                "begin": "2014-10-21 T 08:02.501",
                "end": "2014-10-21 T 08:02.550",
                "pending": 0,
                "active": 0,
                "done": 1,
                "succeeded": 1,
                "failed": 0,
                "ignored": 0
            },
            "delivering": {
                "begin": "2014-10-21 T 08:02.553",
                "end": "",
                "pending": 0,
                "active": 1,
                "done": 0,
                "succeeded": 0,
                "failed": 0,
                "ignored": 0
            }
        }
    }

VS.

.. code-block:: json

    "item": {
        "id": 1,
        "chunkId": 1,
        "jobId": 1234,
        "timeOfCreation": "2014-10-21 T 08:02.456",
        "timeOfCompletion": "2014-10-21 T 08:02.504",
        "timeOfLastModification": "2014-10-21 T 08:02.504",

        "contents": {
            "chunkifying": {
                "encoding": "utf-8",
                "data": "..."
            },
            "processing": {
                "encoding": "utf-8",
                "data": "..."
            },
            "delivering": {
                "encoding": "utf-8",
                "data": "..."
            }
        }

        "state": {
            "chunkifying": "succeeded|failed|ignored",
            "processing": "succeeded|failed|ignored",
            "delivering": "succeeded|failed|ignored"
        }
    }

