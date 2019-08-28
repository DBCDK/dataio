INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (1, '1', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'jobstore',
           '2015-05-07 12:55:22.240253', 0, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (2, '2', 'unfinished chunks index', 'SQL', 'V2__unfinished_chunks_index.sql', -1544381911, 'jobstore',
           '2015-05-07 12:55:22.484844', 103, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (3, '3', 'sink reference index', 'SQL', 'V3__sink_reference_index.sql', -1719960924, 'jobstore',
           '2015-06-13 00:34:11.375085', 254, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (4, '4', 'job specification json to jsonb', 'SQL', 'V4__job_specification_json_to_jsonb.sql', -1970508718,
           'jobstore', '2015-06-25 00:34:09.663748', 1605, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (5, '5', 'json append functions', 'SQL', 'V5__json_append_functions.sql', 1426897880, 'jobstore',
           '2015-06-26 00:02:22.824431', 195, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (6, '6', 'job specification append type', 'SQL', 'V6__job_specification_append_type.sql', -863098778,
           'jobstore', '2015-06-26 00:02:27.383535', 4541, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (7, '7', 'item nextprocessingoutcome', 'SQL', 'V7__item_nextprocessingoutcome.sql', 106404380, 'jobstore',
           '2015-07-16 03:35:31.336854', 95, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (8, '8', 'item nextprocessingoutcome reset', 'SQL', 'V8__item_nextprocessingoutcome_reset.sql', 1132927666,
           'jobstore', '2015-08-04 03:49:23.207131', 790446, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES
    (9, '9', 'job fatalerror', 'SQL', 'V9__job_fatalerror.sql', 1488249119, 'jobstore', '2015-08-29 03:33:58.967905',
        2894, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (10, '10', 'job fatalerror index', 'SQL', 'V10__job_fatalerror_index.sql', -1837118084, 'jobstore',
            '2015-08-29 03:33:59.137638', 89, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES
    (11, '11', 'alter job fatalerror index', 'SQL', 'V11__alter_job_fatalerror_index.sql', -775488899, 'jobstore',
         '2015-09-01 03:34:33.48226', 273, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (12, '12', 'notification table', 'SQL', 'V12__notification_table.sql', -1164755599, 'jobstore',
            '2015-09-10 03:33:48.318809', 447, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (13, '13', 'notification table destination column can be null column jobid indexed', 'SQL',
            'V13__notification_table_destination_column_can_be_null_column_jobid_indexed.sql', 1632625037, 'jobstore',
            '2015-09-16 22:49:24.444419', 268, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (14, '14', 'jobstore jobqueue create table', 'SQL', 'V14__jobstore_jobqueue_create_table.sql', -2082292582,
            'jobstore', '2015-09-23 03:34:01.297965', 486, TRUE);
INSERT INTO schema_version (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (15, '15', 'notification table add context column', 'SQL', 'V15__notification_table_add_context_column.sql',
            -1417072885, 'jobstore', '2015-10-28 03:35:46.515305', 139, TRUE);

insert into job (id, specification, state, flowstorereferences ) values ( 39098, '{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences ) values ( 39044, '{}'::JSONB, '{}'::JSON, '{}'::JSON);


insert into chunk (id, jobid, datafileid, sequenceanalysisdata, state ) values (0, 39098, ' ','{}'::JSON,'{}'::JSON);
insert into chunk (id, jobid, datafileid, sequenceanalysisdata, state ) values (0, 39044, ' ','{}'::JSON,'{}'::JSON);


INSERT INTO item (id, chunkid, jobid, timeofcreation, timeofcompletion, timeoflastmodification, state, partitioningoutcome, processingoutcome, deliveringoutcome, nextprocessingoutcome)
VALUES (6, 0, 39098, '2015-10-30 13:20:38.332217', '2015-10-30 13:20:38.444', '2015-10-30 13:20:38.459995',
           '{"states":{"PROCESSING":{"beginDate":1446207638393,"endDate":1446207638393,"succeeded":1,"failed":0,"ignored":0},"DELIVERING":{"beginDate":1446207638444,"endDate":1446207638444,"succeeded":1,"failed":0,"ignored":0},"PARTITIONING":{"beginDate":1446207638331,"endDate":1446207638331,"succeeded":1,"failed":0,"ignored":0}},"diagnostics":[]}',
           '{"data":"PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0nVVRGLTgnPz48Y29udGFpbmVyPgogICAgPHJlY29yZD5kYXRhNjwvcmVjb3JkPgogICAgPC9jb250YWluZXI+","encoding":"UTF-8"}',
           '{"data":"SGVsbG8gZnJvbSBqYXZhc2NyaXB0IQo=","encoding":"UTF-8"}',
           '{"data":"U2V0IGJ5IER1bW15U2luaw==","encoding":"UTF-8"}', 'null');
INSERT INTO item (id, chunkid, jobid, timeofcreation, timeofcompletion, timeoflastmodification, state, partitioningoutcome, processingoutcome, deliveringoutcome, nextprocessingoutcome)
VALUES (0, 0, 39098, '2015-10-30 13:20:38.332217', '2015-10-30 13:20:38.443', '2015-10-30 13:20:38.44968',
           '{"states":{"PROCESSING":{"beginDate":1446207638392,"endDate":1446207638392,"succeeded":1,"failed":0,"ignored":0},"DELIVERING":{"beginDate":1446207638443,"endDate":1446207638443,"succeeded":1,"failed":0,"ignored":0},"PARTITIONING":{"beginDate":1446207638328,"endDate":1446207638329,"succeeded":1,"failed":0,"ignored":0}},"diagnostics":[]}',
           '{"data":"PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0nVVRGLTgnPz48Y29udGFpbmVyPgogICAgPHJlY29yZD5kYXRhMDwvcmVjb3JkPgogICAgPC9jb250YWluZXI+","encoding":"UTF-8"}',
           '{"data":"SGVsbG8gZnJvbSBqYXZhc2NyaXB0IQo=","encoding":"UTF-8"}',
           '{"data":"U2V0IGJ5IER1bW15U2luaw==","encoding":"UTF-8"}', 'null');
INSERT INTO item (id, chunkid, jobid, timeofcreation, timeofcompletion, timeoflastmodification, state, partitioningoutcome, processingoutcome, deliveringoutcome, nextprocessingoutcome)
VALUES (3, 0, 39098, '2015-10-30 13:20:38.332217', '2015-10-30 13:20:38.444', '2015-10-30 13:20:38.452915',
           '{"states":{"PROCESSING":{"beginDate":1446207638393,"endDate":1446207638393,"succeeded":1,"failed":0,"ignored":0},"DELIVERING":{"beginDate":1446207638444,"endDate":1446207638444,"succeeded":1,"failed":0,"ignored":0},"PARTITIONING":{"beginDate":1446207638330,"endDate":1446207638330,"succeeded":1,"failed":0,"ignored":0}},"diagnostics":[]}',
           '{"data":"PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0nVVRGLTgnPz48Y29udGFpbmVyPgogICAgPHJlY29yZD5kYXRhMzwvcmVjb3JkPgogICAgPC9jb250YWluZXI+","encoding":"UTF-8"}',
           '{"data":"SGVsbG8gZnJvbSBqYXZhc2NyaXB0IQo=","encoding":"UTF-8"}',
           '{"data":"U2V0IGJ5IER1bW15U2luaw==","encoding":"UTF-8"}', 'null');
INSERT INTO item (id, chunkid, jobid, timeofcreation, timeofcompletion, timeoflastmodification, state, partitioningoutcome, processingoutcome, deliveringoutcome, nextprocessingoutcome)
VALUES (4, 0, 39098, '2015-10-30 13:20:38.332217', '2015-10-30 13:20:38.444', '2015-10-30 13:20:38.457725',
           '{"states":{"PROCESSING":{"beginDate":1446207638393,"endDate":1446207638393,"succeeded":1,"failed":0,"ignored":0},"DELIVERING":{"beginDate":1446207638444,"endDate":1446207638444,"succeeded":1,"failed":0,"ignored":0},"PARTITIONING":{"beginDate":1446207638330,"endDate":1446207638331,"succeeded":1,"failed":0,"ignored":0}},"diagnostics":[]}',
           '{"data":"PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0nVVRGLTgnPz48Y29udGFpbmVyPgogICAgPHJlY29yZD5kYXRhNDwvcmVjb3JkPgogICAgPC9jb250YWluZXI+","encoding":"UTF-8"}',
           '{"data":"SGVsbG8gZnJvbSBqYXZhc2NyaXB0IQo=","encoding":"UTF-8"}',
           '{"data":"U2V0IGJ5IER1bW15U2luaw==","encoding":"UTF-8"}', 'null');
INSERT INTO item (id, chunkid, jobid, timeofcreation, timeofcompletion, timeoflastmodification, state, partitioningoutcome, processingoutcome, deliveringoutcome, nextprocessingoutcome)
VALUES (5, 0, 39098, '2015-10-30 13:20:38.332217', '2015-10-30 13:20:38.444', '2015-10-30 13:20:38.458814',
           '{"states":{"PROCESSING":{"beginDate":1446207638393,"endDate":1446207638393,"succeeded":1,"failed":0,"ignored":0},"DELIVERING":{"beginDate":1446207638444,"endDate":1446207638444,"succeeded":1,"failed":0,"ignored":0},"PARTITIONING":{"beginDate":1446207638331,"endDate":1446207638331,"succeeded":1,"failed":0,"ignored":0}},"diagnostics":[]}',
           '{"data":"PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0nVVRGLTgnPz48Y29udGFpbmVyPgogICAgPHJlY29yZD5kYXRhNTwvcmVjb3JkPgogICAgPC9jb250YWluZXI+","encoding":"UTF-8"}',
           '{"data":"SGVsbG8gZnJvbSBqYXZhc2NyaXB0IQo=","encoding":"UTF-8"}',
           '{"data":"U2V0IGJ5IER1bW15U2luaw==","encoding":"UTF-8"}', 'null');
INSERT INTO item (id, chunkid, jobid, timeofcreation, timeofcompletion, timeoflastmodification, state, partitioningoutcome, processingoutcome, deliveringoutcome, nextprocessingoutcome)
VALUES (7, 0, 39098, '2015-10-30 13:20:38.332217', '2015-10-30 13:20:38.444', '2015-10-30 13:20:38.461614',
           '{"states":{"PROCESSING":{"beginDate":1446207638393,"endDate":1446207638393,"succeeded":1,"failed":0,"ignored":0},"DELIVERING":{"beginDate":1446207638444,"endDate":1446207638444,"succeeded":1,"failed":0,"ignored":0},"PARTITIONING":{"beginDate":1446207638331,"endDate":1446207638331,"succeeded":1,"failed":0,"ignored":0}},"diagnostics":[]}',
           '{"data":"PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0nVVRGLTgnPz48Y29udGFpbmVyPgogICAgPHJlY29yZD5kYXRhNzwvcmVjb3JkPgo8L2NvbnRhaW5lcj4=","encoding":"UTF-8"}',
           '{"data":"SGVsbG8gZnJvbSBqYXZhc2NyaXB0IQo=","encoding":"UTF-8"}',
           '{"data":"U2V0IGJ5IER1bW15U2luaw==","encoding":"UTF-8"}', 'null');
INSERT INTO item (id, chunkid, jobid, timeofcreation, timeofcompletion, timeoflastmodification, state, partitioningoutcome, processingoutcome, deliveringoutcome, nextprocessingoutcome)
VALUES (1, 0, 39098, '2015-10-30 13:20:38.332217', '2015-10-30 13:20:38.444', '2015-11-02 13:30:42.636838',
           '{"states":{"PROCESSING":{"beginDate":1446066361686,"endDate":1446066361686,"succeeded":0,"failed":1,"ignored":0},"DELIVERING":{"beginDate":1446066362025,"endDate":1446066362027,"succeeded":0,"failed":1,"ignored":0},"PARTITIONING":{"beginDate":1446066361034,"endDate":1446066361034,"succeeded":1,"failed":0,"ignored":0}},"diagnostics":[]}',
           '{"data":"PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0nVVRGLTgnPz48Y29udGFpbmVyPgogICAgPHJlY29yZD5kYXRhMTwvcmVjb3JkPgogICAgPC9jb250YWluZXI+","encoding":"UTF-8"}',
           '{"data":"SGVsbG8gZnJvbSBqYXZhc2NyaXB0IQo=","encoding":"UTF-8"}',
           '{"data":"U2V0IGJ5IER1bW15U2luaw==","encoding":"UTF-8"}', 'null');
INSERT INTO item (id, chunkid, jobid, timeofcreation, timeofcompletion, timeoflastmodification, state, partitioningoutcome, processingoutcome, deliveringoutcome, nextprocessingoutcome)
VALUES (2, 0, 39098, '2015-10-30 13:20:38.332217', '2015-10-30 13:20:38.444', '2015-11-02 13:30:42.643815',
           '{"states":{"PROCESSING":{"beginDate":1446066361686,"endDate":1446066361686,"succeeded":0,"failed":0,"ignored":1},"DELIVERING":{"beginDate":1446066362025,"endDate":1446066362027,"succeeded":0,"failed":1,"ignored":0},"PARTITIONING":{"beginDate":1446066361034,"endDate":1446066361034,"succeeded":1,"failed":0,"ignored":0}},"diagnostics":[]}',
           '{"data":"PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0nVVRGLTgnPz48Y29udGFpbmVyPgogICAgPHJlY29yZD5kYXRhMjwvcmVjb3JkPgogICAgPC9jb250YWluZXI+","encoding":"UTF-8"}',
           '{"data":"SGVsbG8gZnJvbSBqYXZhc2NyaXB0IQo=","encoding":"UTF-8"}',
           '{"data":"U2V0IGJ5IER1bW15U2luaw==","encoding":"UTF-8"}', 'null');


INSERT INTO item (id, chunkid, jobid, timeofcreation, timeofcompletion, timeoflastmodification, state, partitioningoutcome, processingoutcome, deliveringoutcome, nextprocessingoutcome)
VALUES (0, 0, 39044, '2015-10-26 12:29:21.096841', NULL, '2015-10-26 12:29:21.097821',
           '{"states":{"PROCESSING":{"beginDate":null,"endDate":null,"succeeded":0,"failed":0,"ignored":0},"DELIVERING":{"beginDate":null,"endDate":null,"succeeded":0,"failed":0,"ignored":0},"PARTITIONING":{"beginDate":1445858961064,"endDate":1445858961095,"succeeded":0,"failed":1,"ignored":0}},"diagnostics":[{"level":"FATAL","message":"Unable to complete partitioning at chunk 0 item 0: Specified encoding not supported: ''utf8'' ","stacktrace":"dk.dbc.dataio.jobstore.types.InvalidEncodingException: Specified encoding not supported: ''utf8'' \n\tat dk.dbc.dataio.jobstore.service.partitioner.Iso2709DataPartitionerFactory$Iso2709DataPartitioner.validateSpecifiedEncoding(Iso2709DataPartitionerFactory.java:147)\n\tat dk.dbc.dataio.jobstore.service.partitioner.Iso2709DataPartitionerFactory$Iso2709DataPartitioner.iterator(Iso2709DataPartitionerFactory.java:102)\n\tat dk.dbc.dataio.jobstore.service.ejb.PgJobStoreRepository.createChunkItemEntities(PgJobStoreRepository.java:566)\n\tat dk.dbc.dataio.jobstore.service.ejb.PgJobStoreRepository.createChunkEntity(PgJobStoreRepository.java:230)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:497)\n\tat org.glassfish.ejb.security.application.EJBSecurityManager.runMethod(EJBSecurityManager.java:1081)\n\tat org.glassfish.ejb.security.application.EJBSecurityManager.invoke(EJBSecurityManager.java:1153)\n\tat com.sun.ejb.containers.BaseContainer.invokeBeanMethod(BaseContainer.java:4786)\n\tat com.sun.ejb.EjbInvocation.invokeBeanMethod(EjbInvocation.java:656)\n\tat com.sun.ejb.containers.interceptors.AroundInvokeChainImpl.invokeNext(InterceptorManager.java:822)\n\tat com.sun.ejb.EjbInvocation.proceed(EjbInvocation.java:608)\n\tat dk.dbc.dataio.commons.types.interceptor.StopwatchInterceptor.time(StopwatchInterceptor.java:46)\n\tat sun.reflect.GeneratedMethodAccessor2091.invoke(Unknown Source)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:497)\n\tat com.sun.ejb.containers.interceptors.AroundInvokeInterceptor.intercept(InterceptorManager.java:883)\n\tat com.sun.ejb.containers.interceptors.AroundInvokeChainImpl.invokeNext(InterceptorManager.java:822)\n\tat com.sun.ejb.EjbInvocation.proceed(EjbInvocation.java:608)\n\tat org.jboss.weld.ejb.AbstractEJBRequestScopeActivationInterceptor.aroundInvoke(AbstractEJBRequestScopeActivationInterceptor.java:46)\n\tat org.jboss.weld.ejb.SessionBeanInterceptor.aroundInvoke(SessionBeanInterceptor.java:52)\n\tat sun.reflect.GeneratedMethodAccessor1601.invoke(Unknown Source)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:497)\n\tat com.sun.ejb.containers.interceptors.AroundInvokeInterceptor.intercept(InterceptorManager.java:883)\n\tat com.sun.ejb.containers.interceptors.AroundInvokeChainImpl.invokeNext(InterceptorManager.java:822)\n\tat com.sun.ejb.EjbInvocation.proceed(EjbInvocation.java:608)\n\tat com.sun.ejb.containers.interceptors.SystemInterceptorProxy.doCall(SystemInterceptorProxy.java:163)\n\tat com.sun.ejb.containers.interceptors.SystemInterceptorProxy.aroundInvoke(SystemInterceptorProxy.java:140)\n\tat sun.reflect.GeneratedMethodAccessor1934.invoke(Unknown Source)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:497)\n\tat com.sun.ejb.containers.interceptors.AroundInvokeInterceptor.intercept(InterceptorManager.java:883)\n\tat com.sun.ejb.containers.interceptors.AroundInvokeChainImpl.invokeNext(InterceptorManager.java:822)\n\tat com.sun.ejb.containers.interceptors.InterceptorManager.intercept(InterceptorManager.java:369)\n\tat com.sun.ejb.containers.BaseContainer.__intercept(BaseContainer.java:4758)\n\tat com.sun.ejb.containers.BaseContainer.intercept(BaseContainer.java:4746)\n\tat com.sun.ejb.containers.EJBLocalObjectInvocationHandler.invoke(EJBLocalObjectInvocationHandler.java:212)\n\tat com.sun.ejb.containers.EJBLocalObjectInvocationHandlerDelegate.invoke(EJBLocalObjectInvocationHandlerDelegate.java:88)\n\tat com.sun.proxy.$Proxy424433.createChunkEntity(Unknown Source)\n\tat dk.dbc.dataio.jobstore.service.ejb.__EJB31_Generated__PgJobStoreRepository__Intf____Bean__.createChunkEntity(Unknown Source)\n\tat dk.dbc.dataio.jobstore.service.ejb.PgJobStore.doPartitioningToChunksAndItems(PgJobStore.java:249)\n\tat dk.dbc.dataio.jobstore.service.ejb.PgJobStore.handlePartitioning(PgJobStore.java:175)\n\tat dk.dbc.dataio.jobstore.service.ejb.PgJobStore.handlePartitioningAsynchronously(PgJobStore.java:142)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:497)\n\tat org.glassfish.ejb.security.application.EJBSecurityManager.runMethod(EJBSecurityManager.java:1081)\n\tat org.glassfish.ejb.security.application.EJBSecurityManager.invoke(EJBSecurityManager.java:1153)\n\tat com.sun.ejb.containers.BaseContainer.invokeBeanMethod(BaseContainer.java:4786)\n\tat com.sun.ejb.EjbInvocation.invokeBeanMethod(EjbInvocation.java:656)\n\tat com.sun.ejb.containers.interceptors.AroundInvokeChainImpl.invokeNext(InterceptorManager.java:822)\n\tat com.sun.ejb.EjbInvocation.proceed(EjbInvocation.java:608)\n\tat org.jboss.weld.ejb.AbstractEJBRequestScopeActivationInterceptor.aroundInvoke(AbstractEJBRequestScopeActivationInterceptor.java:55)\n\tat org.jboss.weld.ejb.SessionBeanInterceptor.aroundInvoke(SessionBeanInterceptor.java:52)\n\tat sun.reflect.GeneratedMethodAccessor1601.invoke(Unknown Source)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:497)\n\tat com.sun.ejb.containers.interceptors.AroundInvokeInterceptor.intercept(InterceptorManager.java:883)\n\tat com.sun.ejb.containers.interceptors.AroundInvokeChainImpl.invokeNext(InterceptorManager.java:822)\n\tat com.sun.ejb.EjbInvocation.proceed(EjbInvocation.java:608)\n\tat com.sun.ejb.containers.interceptors.SystemInterceptorProxy.doCall(SystemInterceptorProxy.java:163)\n\tat com.sun.ejb.containers.interceptors.SystemInterceptorProxy.aroundInvoke(SystemInterceptorProxy.java:140)\n\tat sun.reflect.GeneratedMethodAccessor1934.invoke(Unknown Source)\n\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n\tat java.lang.reflect.Method.invoke(Method.java:497)\n\tat com.sun.ejb.containers.interceptors.AroundInvokeInterceptor.intercept(InterceptorManager.java:883)\n\tat com.sun.ejb.containers.interceptors.AroundInvokeChainImpl.invokeNext(InterceptorManager.java:822)\n\tat com.sun.ejb.containers.interceptors.InterceptorManager.intercept(InterceptorManager.java:369)\n\tat com.sun.ejb.containers.BaseContainer.__intercept(BaseContainer.java:4758)\n\tat com.sun.ejb.containers.BaseContainer.intercept(BaseContainer.java:4746)\n\tat com.sun.ejb.containers.EjbAsyncTask.call(EjbAsyncTask.java:101)\n\tat java.util.concurrent.FutureTask.run(FutureTask.java:266)\n\tat java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)\n\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)\n\tat java.lang.Thread.run(Thread.java:745)\n"}]}',
           'null', 'null', 'null', 'null');
