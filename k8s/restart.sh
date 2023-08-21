kubectl apply -f config.yaml
kubectl apply -f secret.yaml

# does not have to be a part of restart as all data would be lost
# but must be a part of a new deployment
if [ $# -gt 0 ] && [ $1 = "-all" ]; then
  kubectl apply -f databases.yaml;
fi

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

k delete deployment clients
k delete deployment books
k delete deployment carts
k delete deployment storage
k delete deployment orders
k delete deployment payments
k delete deployment dynapay
k delete deployment superpay
k delete deployment ratings
k delete deployment ingest