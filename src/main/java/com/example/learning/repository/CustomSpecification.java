package com.example.learning.repository;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CustomSpecification {
    public static <T> SpecificationBuilder<T> builder() {
        return new SpecificationBuilder<>();
    }

    public static <T> SpecificationBuilder<T> builder(Class<T> clazz) {
        return new SpecificationBuilder<>(clazz);
    }

    public static class SpecificationBuilder<T> {
        private final List<Specification<T>> orSpecifications = new ArrayList<>();
        private final List<Specification<T>> andSpecifications = new ArrayList<>();
        private List<Specification<T>> currentPredicates = andSpecifications;
        private Class<T> tClass;

        public SpecificationBuilder(Class tClass) {
            this.tClass = tClass;
        }

        public SpecificationBuilder() {

        }

        public SpecificationBuilder<T> search() {
            currentPredicates = orSpecifications;
            return this;
        }

        public SpecificationBuilder<T> filter() {
            currentPredicates = andSpecifications;
            return this;
        }

        public SpecificationBuilder<T> isLike(String fieldName, String value) {
            if (Objects.nonNull(value)) {
                Specification<T> spec = (root, query, criteriaBuilder) ->
                        criteriaBuilder.like(root.get(fieldName), "%" + value + "%");
                currentPredicates.add(spec);
            }
            return this;
        }

        public SpecificationBuilder<T> isEquals(String fieldName, Object value) {
            if (Objects.nonNull(value)) {
                Specification<T> spec = (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get(fieldName), value);
                currentPredicates.add(spec);
            }
            return this;
        }

        public Specification<T> build() {
            Specification<T> specification = Specification.where(null);

            if (!orSpecifications.isEmpty()) {
                Specification<T> orSpec = orSpecifications.getFirst();
                for (int i = 1; i < orSpecifications.size(); i++) {
                    orSpec = orSpec.or(orSpecifications.get(i));
                }
                specification = specification.and(orSpec);
            }

            if (!andSpecifications.isEmpty()) {
                Specification<T> andSpec = andSpecifications.getFirst();
                for (int i = 1; i < andSpecifications.size(); i++) {
                    andSpec = andSpec.and(andSpecifications.get(i));
                }
                specification = specification.and(andSpec);
            }
            return specification;
        }
    }
}
