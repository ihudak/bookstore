/**
 * Stating configuration for kubernetes withOUT Ingress Controller
 * It's made for docker desktop.
 * Each service must be accessible by the "http://localhost:<port>" URL on your host
 *    e.g. kubernetes services must be of "LoadBalanceer" kind
 */

const baseSrvUrl = 'http://localhost';
export const environment = {
  production: false,
  clientsSrvUrl: `${baseSrvUrl}:81`,
  booksSrvUrl: `${baseSrvUrl}:82`,
  cartsSrvUrl: `${baseSrvUrl}:83`,
  storageSrvUrl: `${baseSrvUrl}:84`,
  ordersSrvUrl: `${baseSrvUrl}:85`,
  ratingsSrvUrl: `${baseSrvUrl}:88`,
  paymentsSrvUrl: `${baseSrvUrl}:86`,
  dynapatSrvUrl: `${baseSrvUrl}:87`,
  ingestSrvUrl: `${baseSrvUrl}:89`,
  verGUI: '1.1.0.staging',
  dateGUI: 'Oct-29-2023'
};
