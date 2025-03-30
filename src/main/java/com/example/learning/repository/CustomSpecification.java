package com.example.learning.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

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
        private final Map<String, Join<T, ?>> joins = new HashMap<>();


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

        public <R> Join<T, R> join(String joinField, JoinType joinType) {
            return (Join<T, R>) joins.computeIfAbsent(joinField, key -> (root, query, criteriaBuilder) -> root.join(joinField, joinType));
        }

//        public <R> SpecificationBuilder<T> join(Class<R> joinClass, String joinField,
//                                                String fieldName, Object value, JoinType joinType) {
//            if (Objects.nonNull(value)) {
//                Specification<T> spec = (root, query, criteriaBuilder) -> {
//                    Join<T, R> join = root.join(joinField, joinType);
//                    return criteriaBuilder.equal(join.get(fieldName), value);
//                };
//                currentPredicates.add(spec);
//            }
//            return this;
//        }

//        public <R, V extends Comparable<V>> SpecificationBuilder<T> join(Class<R> joinClass,
//                                                                         String joinField,
//                                                                         String fieldName,
//                                                                         V value,
//                                                                         JoinType joinType) {
//            if (Objects.nonNull(value)) {
//                Specification<T> spec = (root, query, criteriaBuilder) -> {
//                    Join<T, R> join = root.join(joinField, joinType);
//                    Path<V> path = join.get(fieldName); // Ép kiểu
//                    return criteriaBuilder.greaterThanOrEqualTo(path, value);
//                };
//                currentPredicates.add(spec);
//            }
//            return this;
//        }

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
