/**
 * Development configuration to be executed directly on the workstation, without kubernetes
 * Replace baseSrvUrl with your Ingress URL if you need to deploy on other env
 * Each service must be accessible by the "http://localhost:<port>" URL on your host
 *    e.g. kubernetes services must be of "LoadBalanceer" kind
 */

const baseSrvUrl = 'http://localhost';
export const environment = {
  production: false,
  clientsSrvUrl: `${baseSrvUrl}:8081`,
  booksSrvUrl: `${baseSrvUrl}:8082`,
  cartsSrvUrl: `${baseSrvUrl}:8083`,
  storageSrvUrl: `${baseSrvUrl}:8084`,
  ordersSrvUrl: `${baseSrvUrl}:8085`,
  ratingsSrvUrl: `${baseSrvUrl}:8088`,
  paymentsSrvUrl: `${baseSrvUrl}:8086`,
  dynapatSrvUrl: `${baseSrvUrl}:8087`,
  ingestSrvUrl: `${baseSrvUrl}:8089`,
  verGUI: '1.0.2',
  dateGUI: 'Oct-28-2023'
};
