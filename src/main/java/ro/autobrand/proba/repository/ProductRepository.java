package ro.autobrand.proba.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.autobrand.proba.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
