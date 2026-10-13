package com.dynatrace.ratings.repository;

import com.dynatrace.ratings.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByEmail(String email);
    List<Rating> findByIsbn(String isbn);
    // plain equality: the database collation decides letter case, as the unique constraint does
    Optional<Rating> findByEmailAndIsbn(String email, String isbn);

    @Query("select new com.dynatrace.ratings.repository.IsbnTotals(r.isbn, count(r), sum(r.rating)) from Rating r where r.isbn = :isbn group by r.isbn")
    Optional<IsbnTotals> totalsFor(String isbn);

    @Query("select new com.dynatrace.ratings.repository.IsbnTotals(r.isbn, count(r), sum(r.rating)) from Rating r group by r.isbn order by r.isbn")
    List<IsbnTotals> totalsForAll();
}
