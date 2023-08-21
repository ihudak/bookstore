create database dt_books_clients;
create database dt_books_carts;
create database dt_books_payments;
create database dt_books_dynapays;
create database dt_books_ratings;

grant all privileges on dt_books_clients.* to user@'%';
grant all privileges on dt_books_carts.* to user@'%';
grant all privileges on dt_books_payments.* to user@'%';
grant all privileges on dt_books_dynapays.* to user@'%';
grant all privileges on dt_books_ratings.* to user@'%';

flush privileges;
