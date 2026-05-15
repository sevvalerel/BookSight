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
			if (bookRepository.count() < 80) {
				System.out.println(">>> Kitaplar yükleniyor...");
				try {
					// Sabahattin Ali
					googleBooksService.fetchAndSaveTurkishBooks("Kürk Mantolu Madonna Sabahattin Ali", 3);
					googleBooksService.fetchAndSaveTurkishBooks("İçimizdeki Şeytan Sabahattin Ali", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Kuyucaklı Yusuf Sabahattin Ali", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Değirmen Sabahattin Ali", 3);
					// Orhan Pamuk
					googleBooksService.fetchAndSaveTurkishBooks("Benim Adım Kırmızı Orhan Pamuk", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Kar Orhan Pamuk", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Masumiyet Müzesi Orhan Pamuk", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Beyaz Kale Orhan Pamuk", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Sessiz Ev Orhan Pamuk", 3);
					// Elif Şafak
					googleBooksService.fetchAndSaveTurkishBooks("Aşk Elif Şafak", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Pinhan Elif Şafak", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Araf Elif Şafak", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Ustam ve Ben Elif Şafak", 3);
					googleBooksService.fetchAndSaveTurkishBooks("İskender Elif Şafak", 3);
					// Yaşar Kemal
					googleBooksService.fetchAndSaveTurkishBooks("İnce Memed Yaşar Kemal", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Teneke Yaşar Kemal", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Ortadirek Yaşar Kemal", 3);
					// Reşat Nuri Güntekin
					googleBooksService.fetchAndSaveTurkishBooks("Çalıkuşu Reşat Nuri Güntekin", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Yaprak Dökümü Reşat Nuri Güntekin", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Acımak Reşat Nuri Güntekin", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Yeşil Gece Reşat Nuri Güntekin", 3);
					// Halide Edib Adıvar
					googleBooksService.fetchAndSaveTurkishBooks("Sinekli Bakkal Halide Edib Adıvar", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Ateşten Gömlek Halide Edib Adıvar", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Vurun Kahpeye Halide Edib Adıvar", 3);
					// Ahmet Hamdi Tanpınar
					googleBooksService.fetchAndSaveTurkishBooks("Huzur Ahmet Hamdi Tanpınar", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Saatleri Ayarlama Enstitüsü Tanpınar", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Beş Şehir Tanpınar", 3);
					// Oğuz Atay
					googleBooksService.fetchAndSaveTurkishBooks("Tutunamayanlar Oğuz Atay", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Tehlikeli Oyunlar Oğuz Atay", 3);
					// Peyami Safa
					googleBooksService.fetchAndSaveTurkishBooks("Dokuzuncu Hariciye Koğuşu Peyami Safa", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Fatih Harbiye Peyami Safa", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Matmazel Noraliya Peyami Safa", 3);
					// Kemal Tahir
					googleBooksService.fetchAndSaveTurkishBooks("Devlet Ana Kemal Tahir", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Yorgun Savaşçı Kemal Tahir", 3);
					// Çeviri Klasikler
					googleBooksService.fetchAndSaveTurkishBooks("Suç ve Ceza Dostoyevski Türkçe", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Sefiller Victor Hugo Türkçe", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Şeker Portakalı Jose Mauro", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Küçük Prens Antoine Türkçe", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Martin Eden Jack London Türkçe", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Anna Karenina Tolstoy Türkçe", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Uçurtma Avcısı Khaled Hosseini Türkçe", 3);
					googleBooksService.fetchAndSaveTurkishBooks("Simyacı Paulo Coelho Türkçe", 3);
					System.out.println(">>> Kitaplar yüklendi! Toplam: " + bookRepository.count());
				} catch (Exception e) {
					System.out.println(">>> Google Books API hatası, uygulama devam ediyor: " + e.getMessage());
				}
			} else {
				System.out.println(">>> DB'de zaten " + bookRepository.count() + " kitap var.");
			}
		};
	}
}