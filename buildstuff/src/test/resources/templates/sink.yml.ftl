apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: ${serviceName}
    app.dbc.dk/team: metascrum
    app.kubernetes.io/component: service
    app.kubernetes.io/name: ${name}
    app.kubernetes.io/part-of: dataio
  name: ${serviceName}
spec:
  progressDeadlineSeconds: ${progressDeadline}
  replicas: ${replicas}
  selector:
    matchLabels: {app: ${serviceName}}
  strategy:
    <#if !deploymentType?? || deploymentType == "RollingUpdate">
    rollingUpdate: {maxSurge: 1, maxUnavailable: 0}
    </#if>
    type: ${deploymentType!"RollingUpdate"}
  template:
    metadata:
      labels:
        app: ${serviceName}
        app.dbc.dk/team: metascrum
        app.kubernetes.io/component: service
        app.kubernetes.io/name: ${name}
        app.kubernetes.io/part-of: dataio
        network-policy-dmzproxy-outgoing: 'yes'
        network-policy-http-incoming: 'yes'
        network-policy-http-outgoing: 'yes'
        network-policy-artemis-outgoing: 'yes'
        network-policy-payara-incoming: 'yes'
        network-policy-postgres-outgoing: "yes"
        promaas.enable: "true"
      annotations:
        promaas.scrape.port: "8080"
    spec:
      containers:
      - env:
        - name: ARTEMIS_MQ_HOST
          value: ${jmsHost}
        - name: ARTEMIS_ADMIN_PORT
          value: '8161'
        - name: ARTEMIS_USER
          value: admin
        - name: ARTEMIS_PASSWORD
          value: test
        - {name: QUEUE, value: ${queue}}
        - name: CONSUMER_THREADS
          value: '${consumerThreads!1}'
        - name: MAX_HEAP
          value: ${maxHeap}
        - name: FILESTORE_URL
          value: ${filestore}
        - name: JOBSTORE_URL
          value: ${jobstore}
      <#list envProperties!{} as key, value>
        - name: ${key}
          value: '${value}'
      </#list>
        image: docker-metascrum.artifacts.dbccloud.dk/${image}
        name: ${serviceName}
        resources:
          requests:
            cpu: "${cpu}"
            memory: "${mem}"
        ports:
        - {containerPort: 8080, protocol: TCP}
        startupProbe:
          failureThreshold: 120
          httpGet:
            path: ${probePath!'/health'}
            port: 8080
          periodSeconds: 1
        livenessProbe:
          failureThreshold: 6
          httpGet:
            path: ${probePath!'/health'}
            port: 8080
          initialDelaySeconds: 15
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  labels:
    app: ${serviceName}
    app.dbc.dk/team: metascrum
    app.kubernetes.io/component: service
    app.kubernetes.io/name: ${name}
    app.kubernetes.io/part-of: dataio
    healthcheck.type: http
  <#if serviceHealthcheck>
  annotations:
    healthcheck.path: ${probePath!"/health"}
    healthcheck.severity: "warning"
  </#if>
  name: ${serviceName}
spec:
  ports:
  - {name: http, port: 80, protocol: TCP, targetPort: 8080}
  selector: {app: ${serviceName}}
  type: ClusterIP
