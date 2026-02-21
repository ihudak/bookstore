az aks get-credentials --resource-group <RESOURCE_GROUP_NAME> --name <AKS_CLUSTER_NAME>

helm install dynatrace-operator oci://public.ecr.aws/dynatrace/dynatrace-operator --create-namespace --namespace dynatrace --atomic

kubectl apply -f dynakube.yaml


helm install ingress-nginx ingress-nginx/ingress-nginx --create-namespace --namespace ingess-nginx --set controller.service.annotations."service\.beta\.kubernetes\.io/azure-load-balancer-health-probe-request-path"=/healthz


kubectl apply -f namespace.yaml
kubectl apply -f config_agents.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml

# does not have to be a part of restart as all data would be lost
# but must be a part of a new deployment
IF "%1"!="-nodb" (
  kubectl apply -f databases.yaml
  timeout 15
)

kubectl apply -f clients.yaml
kubectl apply -f books.yaml
kubectl apply -f carts.yaml
kubectl apply -f storage.yaml
kubectl apply -f orders.yaml
kubectl apply -f ratings.yaml
kubectl apply -f payments.yaml
kubectl apply -f dynapay.yaml
kubectl apply -f ingest.yaml

SET WEB="FALSE"
IF "%1"=="-web" SET WEB="TRUE"
IF "%2"=="-web" SET WEB="TRUE"

IF WEB=="TRUE" (
  kubectl apply -f web.yaml
)

kubectl apply -f ingress.yaml
