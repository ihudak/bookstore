# BookStore - Demo project

Bookstore project consists of 8 primary microservices, one web application, and a load generator.  
**Purposes:** demonstrating of Dynatrace capabilities in high-load and unhealthy environments

> **Note:** to setup a Kubernetes-based environment from public docker images from GCR, 
> please follow the instructions in the k8s/README file

# Build

### Prerequisite
* Docker service (docker desktop) should be running

#### Ingress Controller
Install Ingress controller to enable WebApp access all microservices in your deployment

##### Docker Desktop: Using Helm

    helm upgrade --install ingress-nginx ingress-nginx --repo https://kubernetes.github.io/ingress-nginx --namespace ingress-nginx --create-namespace

> *Note:* Restart Docker Desktop after setting up ingress-nginx

#### Docker Desktop: Using kubectl

    kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml

> *Note:* Restart Docker Desktop after setting up ingress-nginx

#### Azure Kubernetes Service (AKS)

    helm install ingress-nginx ingress-nginx/ingress-nginx --create-namespace --namespace ingess-nginx `
    --set controller.service.annotations."service\.beta\.kubernetes\.io/azure-load-balancer-health-probe-request-path"=/healthz

#### Minikube

    minikube addons enable ingress

### Build on Linux/MacOS/WLS2
* Go to `k8s` directory
* execute, optionally providing otel token with the scopes of `trace_otel_ingest`, `log_otel_ingest`, `metric_otel_ingest`  

        ./build_docker_all.sh [<tenantID>] [dev|sprint] [<otel_ingest_token>]
  >  _the token can be base64-ed or open_

### Build on Windows
* Go to `k8s` directory
* execute, optionally providing otel token with the scopes of `trace_otel_ingest`, `log_otel_ingest`, `metric_otel_ingest`  

         .\build_docker_all.bat [<tenantID>] [dev|sprint] [<otel_ingest_token>]


## Configuration
### ingress
* Set the public host name in `ingress.yaml`
        
        - host: kubernetes.docker.internal

### configmap
* Set your tenant's id and URL in `configmap.yaml`

        DT_ENV_ID: <tenant-id> ## for instance, "fdg3423" for Dynatrace SaaS or "a4a7b494-703c-421b-9f28-cb496d81f834" for Dynatrace Managed
        DT_ENV_URL: <DT tenant url> ## for instance, "https://fdg3423.live.dynatrace.com" for Dynatrace SaaS or https://dynatrace-managed.example.com/e/a4a7b494-703c-421b-9f28-cb496d81f834 for Dynatrace Managed
        ...
        BOOKSTORE_BASE_SRV_URL: "http://<host_from_ingress>/api" ## e.g. BOOKSTORE_BASE_SRV_URL: "http://kubernetes.docker.internal/api"
        ## if you use Labmda function to randomize payment failure/success, please also set the following:
        DT_BANK_CHECK: "lambda" # lambda | rand
        AWS_LAMBDA_URL: <URL to the Lambda function> ## e.g. AWS_LAMBDA_URL: "https://my-lambla.execute-api.us-east-1.amazonaws.com/default/bookstore-bankinfo"

* Define instrumentation (OneAgent or Otel) for the microservices in `config_agents.yaml`  

        <microservice>_agent: [oneAgent|otelAgent|none] # set for all *_agent records

* Generate Tokens  
  * OpenIngest token  
    * Open `Dynatrace UI > Manage > Access tokens`  
    * click on `Generate new token`  
    * select `Ingest OpenTelemetry traces` in the scope  
    * click `Generate`  
    * convert the token to base64:  

            echo -n <token> | base64 -w 0
      > **Note:** on MacOS `-w 0` is not needed

  * OneAgent token  
    * Open `Dynatrace UI > Manage > Access tokens`  
    * click on `Generate new token`  
    * select `PaaS integration - Installer download` in the scope  
    * click `Generate`  
    * convert the token to base64:  

            echo -n <token> | base64 -w 0
      > **Note:** on MacOS `-w 0` is not needed

  * Entities Read token (needed to send deployment events)  
    * Open `Dynatrace UI > Manage > Access tokens`  
    * click on `Generate new token`  
    * select `Read entities` in the scope  
    * click `Generate`  
    * convert the token to base64:  

            echo -n <token> | base64 -w 0
      > **Note:** on MacOS `-w 0` is not needed

  * Events Ingest token (needed for sending deployment events)  
    * Open `Dynatrace UI > Manage > Access tokens`  
    * click on `Generate new token`  
    * select `Ingest events` in the scope  
    * click `Generate`  
    * convert the token to base64:  

            echo -n <token> | base64 -w 0
      > **Note:** on MacOS `-w 0` is not needed

* Put tokens in the secrets, `secret.yaml` file  

      oneagent-token: <base64-ed OneAgent token (make sure you concatenated if it's multiline)>  
      otel-token: <base64-ed Otel token (make sure you concatenated if it's multiline)>  

### deployment's yaml files
the yaml configurations for deployments need to be preconfigured.
Use `preset_deployment.sh` for that:

* **on-deploy agent installation** images for Intel/AMD architecture, books namespace (default ns is bookstore):
```
./preset_deployment.sh -gyes -ax64 -n books
```

* **on-deploy agent installation**  images for ARMv8 architecture, books namespace (default ns is bookstore):
```
./preset_deployment.sh -gyes -aarm -n books
```

* **on-build agent installation** images for Intel/AMD architecture, books namespace (default ns is bookstore):
```
./preset_deployment.sh -gpre -ax64 -n books
```

* **on-build agent installation**  images for ARMv8 architecture, books namespace (default ns is bookstore):
```
./preset_deployment.sh -gpre -aarm -n books
```

* use **non-instrumented images** for Intel/AMD architecture, bookstore namespace (-n to override):
```
./preset_deployment.sh -gno -ax64
```

* use **non-instrumented images** for ARMv8 architecture, bookstore namespace (-n to override):
```
./preset_deployment.sh -gno -aarm
```

* reset the yaml-files:
```
./preset_deployment.sh -reset
```

* "on deploy" vs "on build":
  * `-gyes` parameter makes OneAgent and Otel to be downloaded and configured on every pod start.
    * **pros:** you get the latest agent on every pod restart
    * **cons:** more traffic (download agents); more time for pod to start
  * `-gpre` parameter makes OneAgent and Otel to be a part of docker image.
    * **pros:** quicker to start, less traffic (agent is already in the image)
    * **cons:** to update the Agents you need to rebuild the docker images

## Deployment
### Manual deployment
1. Create `namespace`:

        kubectl apply -f namespace.yaml

2. Create `configmaps`:

        kubectl apply -f configmap.yaml
        kubectl apply -f config_agents.yaml


3. Create `secrets`:

        kubectl apply -f secret.yaml


4. Create `databases` (unless you're using the `k8s-databases` project):

        kubectl apply -f databases.yaml


5. Create all `deployments` and `services`:

        kubectl apply -f clients.yaml
        kubectl apply -f books.yaml
        kubectl apply -f carts.yaml
        kubectl apply -f storage.yaml
        kubectl apply -f orders.yaml
        kubectl apply -f ratings.yaml
        kubectl apply -f payments.yaml
        kubectl apply -f dynapay.yaml
        kubectl apply -f ingest.yaml


6. Create the `web app`:

        kubectl apply -f web.yaml

7. Setup `Ingress`:

        kubectl apply -f ingress.yaml

8. Delete the app:

        kubectl delete -f web.yaml
        kubectl delete -f clients.yaml
        kubectl delete -f books.yaml
        kubectl delete -f carts.yaml
        kubectl delete -f storage.yaml
        kubectl delete -f orders.yaml
        kubectl delete -f ratings.yaml
        kubectl delete -f payments.yaml
        kubectl delete -f dynapay.yaml
        kubectl delete -f ingest.yaml
        kubectl delete -f ingress.yaml
        kubectl delete -f databases.yaml
        kubectl delete -f secret.yaml
        kubectl delete -f config_agents.yaml
        kubectl delete -f configmap.yaml
        kubectl delete -f namespace.yaml

### Automated deployment

#### Linux

    ./restart.sh       # deploys databases and backend microservices
    ./restart.sh -web  # deploys everything including the web app
    ./restart.sh -nodb # won't touch the DB (restart of the DB causes data reset)
    ./delete.sh        # undeploys microservices and the web app (DBs stay)
    ./delete.sh -all   # undeploys everything, including the DBs, configmaps

#### Windows

#### Linux

    .\restart.bat       &REM deploys databases and backend microservices
    .\restart.bat -web  &REM deploys everything including the web app
    .\restart.bat -nodb &REM won't touch the DB (restart of the DB causes data reset)
    .\delete.bat        &REM undeploys microservices and the web app (DBs stay)
    .\delete.bat -all   &REM undeploys everything, including the DBs, configs

#### Connect to pod

    kubectl exec -it <Pod_Name> -c <Container_Name> -- /bin/bash

