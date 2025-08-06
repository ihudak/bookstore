/**
 * Production configuration for kubernetes with Ingress Controller
 * It's made for docker desktop.
 * Replace baseSrvUrl with your Ingress URL if you need to deploy on other env
 */

const baseSrvUrl = 'http://kubernetes.docker.internal/api';
export const environment = {
  production: true,
  clientsSrvUrl: `${baseSrvUrl}/clients`,
  booksSrvUrl: `${baseSrvUrl}/books`,
  cartsSrvUrl: `${baseSrvUrl}/carts`,
  storageSrvUrl: `${baseSrvUrl}/storage`,
  ordersSrvUrl: `${baseSrvUrl}/orders`,
  ratingsSrvUrl: `${baseSrvUrl}/ratings`,
  paymentsSrvUrl: `${baseSrvUrl}/payments`,
  dynapatSrvUrl: `${baseSrvUrl}/dynapay`,
  ingestSrvUrl: `${baseSrvUrl}/ingest`,
  selectedTenant: '',
  verGUI: '1.7.3.prod',
  dateGUI: 'Aug-06-2025'
};
