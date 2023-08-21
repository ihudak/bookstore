import { Component } from '@angular/core';
import {Book} from "../book";
import {BookService} from "../book.service";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'app-update-book',
  templateUrl: './update-book.component.html',
  styleUrls: ['./update-book.component.css']
})
export class UpdateBookComponent {

  id: number = 0;
  book: Book = new Book();
  constructor(private bookService: BookService, private route: ActivatedRoute, private router: Router) {
  }

  onSubmit() {
    this.bookService.updateBook(this.id, this.book).subscribe(data => {
        console.log(data);
        this.goToBooksList();
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.bookService.getBookById(this.id).subscribe(data => {
        this.book = data;
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToBooksList() {
    this.router.navigate(['/books']);
  }
}
