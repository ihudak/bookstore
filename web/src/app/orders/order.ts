export class Order {
  id: number = 0;
  email: string | undefined;
  isbn: string | undefined;
  quantity: number = 1;
  price: number = 0.0;
  completed: boolean = false;
  createdAt: string | undefined;
  updatedAt: string | undefined;
}
