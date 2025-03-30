package com.example.learning.specification;

import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpecificationBuilder<T> {
    private List<Specification<T>> orSpecifications;
    private List<Specification<T>> andSpecifications;
    private List<Specification<T>> currentPredicates;

    public SpecificationBuilder<T> builder() {
        orSpecifications = new ArrayList<>();
        andSpecifications = new ArrayList<>();
        currentPredicates = andSpecifications;
        return this;
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
