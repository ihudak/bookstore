import { Component } from '@angular/core';
import {Router} from "@angular/router";
import {Book} from "../book";
import {BookService} from "../book.service";

@Component({
  selector: 'app-create-book',
  templateUrl: './create-book.component.html',
  styleUrls: ['./create-book.component.css']
})
export class CreateBookComponent {
  book: Book = new Book();
  constructor(private bookService: BookService, private router: Router) {
  }

  saveBook() {
    this.bookService.createBook(this.book).subscribe(data => {
        console.log(data);
        this.goToBooksList();
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToBooksList() {
    this.router.navigate(['/books']);
  }

  onSubmit(){
    this.saveBook();
  }
}
