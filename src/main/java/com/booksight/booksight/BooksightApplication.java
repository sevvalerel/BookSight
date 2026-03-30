package com.booksight.booksight;

import com.booksight.booksight.repository.BookRepository;
import com.booksight.booksight.service.GoogleBooksService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BooksightApplication {

	public static void main(String[] args) {
		SpringApplication.run(BooksightApplication.class, args);
	}

	@Bean
	public ApplicationRunner loadInitialBooks(GoogleBooksService googleBooksService, BookRepository bookRepository) {
		return args -> {
			if (bookRepository.count() < 10) {
				System.out.println(">>> Kitaplar yükleniyor...");
				googleBooksService.fetchAndSaveTurkishBooks("Kürk Mantolu Madonna", 5);
				googleBooksService.fetchAndSaveTurkishBooks("İçimizdeki Şeytan", 5);
				googleBooksService.fetchAndSaveTurkishBooks("Benim Adım Kırmızı", 5);
				googleBooksService.fetchAndSaveTurkishBooks("Kar Orhan Pamuk", 5);
				googleBooksService.fetchAndSaveTurkishBooks("Araf Elif Şafak", 5);
				googleBooksService.fetchAndSaveTurkishBooks("Pinhan Elif Şafak", 5);
				googleBooksService.fetchAndSaveTurkishBooks("İnce Memed", 5);
				googleBooksService.fetchAndSaveTurkishBooks("Çalıkuşu", 5);
				googleBooksService.fetchAndSaveTurkishBooks("Yaprak Dökümü", 5);
				googleBooksService.fetchAndSaveTurkishBooks("Sinekli Bakkal", 5);
				googleBooksService.fetchAndSaveTurkishBooks("Huzur Ahmet Hamdi", 5);
				googleBooksService.fetchAndSaveTurkishBooks("Tutunamayanlar", 5);
				googleBooksService.fetchAndSaveTurkishBooks("Saatleri Ayarlama Enstitüsü", 5);
				googleBooksService.fetchAndSaveTurkishBooks("Beyaz Kale Pamuk", 5);
				googleBooksService.fetchAndSaveTurkishBooks("Masumiyet Müzesi", 5);
				System.out.println(">>> Kitaplar yüklendi! Toplam: " + bookRepository.count());
			} else {
				System.out.println(">>> DB'de zaten " + bookRepository.count() + " kitap var.");
			}
		};
	}
}