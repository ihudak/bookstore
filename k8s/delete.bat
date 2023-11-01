kubectl delete -f clients.yaml
kubectl delete -f books.yaml
kubectl delete -f carts.yaml
kubectl delete -f storage.yaml
kubectl delete -f orders.yaml
kubectl delete -f ratings.yaml
kubectl delete -f payments.yaml
kubectl delete -f dynapay.yaml
kubectl delete -f ingest.yaml
kubectl delete -f web.yaml

IF "%1"=="-all" (
  kubectl delete -f ingress.yaml
  kubectl delete -f databases.yaml
  kubectl delete -f secret.yaml
  kubectl delete -f config.yaml
  kubectl delete -f namespace.yaml
)
