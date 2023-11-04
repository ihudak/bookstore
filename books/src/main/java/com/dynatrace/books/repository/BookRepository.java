package com.dynatrace.books.repository;

import com.dynatrace.books.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByAuthor(String author);
    Book findByIsbn(String isbn);
    List<Book> findByPublished(boolean isPublished);

    @Query(value = "UPDATE Book SET published = :vend")
    @Modifying
    @Transactional
    int bulkBookVending(boolean vend);

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE books", nativeQuery = true)
    void truncateTable();
}
