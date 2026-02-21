#!/bin/bash
#export NS=boostrore

helm install ingress-nginx ingress-nginx/ingress-nginx --create-namespace --namespace ingess-nginx --set controller.service.annotations."service\.beta\.kubernetes\.io/azure-load-balancer-health-probe-request-path"=/healthz

kubectl apply -f namespace.yaml
kubectl apply -f config_agents.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml

# does not have to be a part of restart as all data would be lost
# but must be a part of a new deployment
if [ $# -eq 0 ] || [ $1 != "-nodb" ]; then
  kubectl apply -f databases.yaml;
  read -t 15 -p "Wait till the databases get up and running...";
  echo;
fi

#kubectl delete deployment clients -n $NS
#kubectl delete deployment books -n $NS
#kubectl delete deployment carts -n $NS
#kubectl delete deployment storage -n $NS
#kubectl delete deployment orders -n $NS
#kubectl delete deployment payments -n $NS
#kubectl delete deployment ratings -n $NS
#kubectl delete deployment dynapay -n $NS
#kubectl delete deployment superpay -n $NS
#kubectl delete deployment ingest -n $NS

kubectl apply -f clients.yaml
kubectl apply -f books.yaml
kubectl apply -f carts.yaml
kubectl apply -f storage.yaml
kubectl apply -f orders.yaml
kubectl apply -f ratings.yaml
kubectl apply -f payments.yaml
kubectl apply -f dynapay.yaml
kubectl apply -f ingest.yaml

if [ "$1" = "-web" ] || [ "$2" = "-web" ]; then
  kubectl apply -f web.yaml;
fi

kubectl apply -f ingress.yaml
