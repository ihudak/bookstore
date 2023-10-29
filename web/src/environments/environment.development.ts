/**
 * Development configuration to be executed directly on the workstation, without kubernetes
 * Replace baseSrvUrl with your Ingress URL if you need to deploy on other env
 * Each service must be accessible by the "http://localhost:<port>" URL on your host
 *    e.g. kubernetes services must be of "LoadBalanceer" kind
 */

const baseSrvUrl = 'http://localhost';
export const environment = {
  production: false,
  clientsSrvUrl: `${baseSrvUrl}/clients`,
  booksSrvUrl: `${baseSrvUrl}/books`,
  cartsSrvUrl: `${baseSrvUrl}/carts`,
  storageSrvUrl: `${baseSrvUrl}/storage`,
  ordersSrvUrl: `${baseSrvUrl}/orders`,
  ratingsSrvUrl: `${baseSrvUrl}/ratings`,
  paymentsSrvUrl: `${baseSrvUrl}/payments`,
  dynapatSrvUrl: `${baseSrvUrl}/dynapay`,
  ingestSrvUrl: `${baseSrvUrl}/ingest`,
  verGUI: '1.1.0.dev',
  dateGUI: 'Oct-29-2023'
};
