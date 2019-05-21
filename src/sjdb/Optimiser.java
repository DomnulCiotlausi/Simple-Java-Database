package sjdb;

import java.util.*;

public class Optimiser implements PlanVisitor {

    private Catalogue catalogue;
    private Set<Attribute> attributes = new HashSet<>();
    private Set<Predicate> predicates = new HashSet<>();
    private Set<Scan> scans = new HashSet<>();
    private Estimator estimator = new Estimator();

    public Optimiser(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    public Operator optimise(Operator plan) {
        plan.accept(this);

        ArrayList<Operator> operators = pushSelectProject(scans, attributes, predicates, plan);
        Operator optimisedPlan = createJoinProduct(predicates, operators, plan);
        return optimisedPlan;
    }

    private ArrayList<Operator> pushSelectProject(Set<Scan> scans, Set<Attribute> attributes, Set<Predicate> predicates, Operator op) {
        ArrayList<Operator> operators = new ArrayList<>();
        for (Scan scan : scans) {
            Operator operator = buildSelects(scan, predicates);
            ArrayList<Predicate> temp = new ArrayList<>();
            temp.addAll(predicates);
            operators.add(this.buildProjects(operator, this.getAttributes(temp, op)));
        }
        return operators;
    }

    private Operator createJoinProduct(Set<Predicate> predicates, ArrayList<Operator> operators, Operator op) {
        ArrayList<Predicate> newPredicates = new ArrayList<>();
        newPredicates.addAll(predicates);

        ArrayList<Operator> temp = new ArrayList<>();
        temp.addAll(operators);
        Operator plan = buildProductJoin(temp, newPredicates, op);
        int cost = estimator.getCost(plan);
        System.out.println("\n" + "Optimised cost: " + cost);
        return plan;
    }

    private Operator buildProductJoin(ArrayList<Operator> operators, ArrayList<Predicate> predicates, Operator op) {
        Operator operator;
        if (operators.size() == 1) {
            operator = operators.get(0);
            if (operator == null) {
                operator.accept(estimator);
            }
            return operator;
        }

        Iterator<Predicate> it = predicates.iterator();
        while (it.hasNext()) {
            Predicate predicate = it.next();
            Operator left = this.getOperatorForAttribute(operators, predicate.getLeftAttribute());
            Operator right = this.getOperatorForAttribute(operators, predicate.getRightAttribute());

            if (left == null) {
                if (right == null) {
                    return null;
                } else {
                    operator = new Select(right, predicate);
                    it.remove();
                }
            } else {
                if (right == null) {
                    operator = new Select(left, predicate);
                    it.remove();
                } else {
                    operator = new Join(left, right, predicate);
                    it.remove();
                }
            }
            if (operator.getOutput() == null) {
                operator.accept(estimator);
            }

            Set<Attribute> neededAttributes = this.getAttributes(predicates, op);
            List<Attribute> possibleAttributes = operator.getOutput().getAttributes();
            if (neededAttributes.equals(possibleAttributes)) {
                operators.add(operator);
            } else {
                List<Attribute> keptAttributes = new ArrayList<>();
                for (Attribute attribute : possibleAttributes) {
                    if (neededAttributes.contains(attribute)) {
                        keptAttributes.add(attribute);
                    }
                }
                if (keptAttributes.isEmpty()) {
                    operators.add(operator);
                } else {
                    Project project = new Project(operator, keptAttributes);
                    project.accept(estimator);
                    operators.add(project);
                }
            }
        }

        while (operators.size() > 1) {
            Operator op1 = operators.get(0);
            Operator op2 = operators.get(1);
            Operator product = new Product(op1, op2);
            product.accept(estimator);

            operators.remove(op1);
            operators.remove(op2);
            operators.add(product);
        }
        return operators.get(0);
    }

    private Operator getOperatorForAttribute(ArrayList<Operator> operators, Attribute attribute) {
        Iterator<Operator> it = operators.iterator();
        while (it.hasNext()) {
            Operator operator = it.next();
            if (operator.getOutput().getAttributes().contains(attribute)) {
                it.remove();
                return operator;
            }
        }
        return null;
    }

    private Operator buildSelects(Operator operator, Set<Predicate> predicates) {
        List<Attribute> attributes = operator.getOutput().getAttributes();
        for (Predicate predicate : predicates) {
            if (operator.getOutput() == null) {
                operator.accept(estimator);
            }
            if ((predicate.equalsValue() && attributes.contains(predicate.getLeftAttribute())) || (!predicate.equalsValue() && attributes.contains(predicate.getLeftAttribute()) && attributes.contains(predicate.getRightAttribute()))) {
                operator = new Select(operator, predicate);
            }
        }
        return operator;
    }

    private Operator buildProjects(Operator operator, Set<Attribute> attributes) {
        if (operator.getOutput() == null) {
            operator.accept(estimator);
        }
        ArrayList<Attribute> att = new ArrayList<>(attributes);
        att.retainAll(operator.getOutput().getAttributes());
        if (att.isEmpty()) {
            return operator;
        } else {
            Operator operator2 = new Project(operator, att);
            operator2.accept(estimator);
            return operator2;
        }
    }

    private Set<Attribute> getAttributes(ArrayList<Predicate> predicates, Operator operator) {
        Set<Attribute> attributes = new HashSet<>();
        for (Predicate predicate : predicates) {
            attributes.add(predicate.getLeftAttribute());
            if (predicate.getRightAttribute() != null) {
                attributes.add(predicate.getRightAttribute());
            }
        }
        if (operator instanceof Project) {
            attributes.addAll(((Project) operator).getAttributes());
        }
        return attributes;
    }

    @Override
    public void visit(Scan op) {
        scans.add(new Scan((NamedRelation) op.getRelation()));
    }

    @Override
    public void visit(Project op) {
        attributes.addAll(op.getAttributes());
    }

    @Override
    public void visit(Select op) {
        predicates.add(op.getPredicate());
        attributes.add(op.getPredicate().getLeftAttribute());
        if (!op.getPredicate().equalsValue()) {
            attributes.add(op.getPredicate().getRightAttribute());
        }
    }

    @Override
    public void visit(Product op) {

    }

    @Override
    public void visit(Join op) {

    }
}
