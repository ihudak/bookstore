export class Book {
  id: number = 0;
  isbn: string = '';
  title: string | undefined;
  author: string | undefined;
  price: number | undefined;
  language: string | undefined;
  published: boolean = false;
}
