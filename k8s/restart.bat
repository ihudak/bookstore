kubectl apply -f namespace.yaml
kubectl apply -f config.yaml
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
kubectl apply -f payments.yaml
kubectl apply -f dynapay.yaml
kubectl apply -f ingest.yaml
kubectl apply -f ratings.yaml
kubectl apply -f bookstore.yaml
