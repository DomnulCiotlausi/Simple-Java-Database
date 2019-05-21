package sjdb;

public class Estimator implements PlanVisitor {

    private int totalCost = 0;

    public Estimator() {
        // empty constructor
    }

    /*
     * Create output relation on Scan operator
     *
     */
    public void visit(Scan op) {
        Relation input = op.getRelation();
        Relation output = new Relation(input.getTupleCount());

        for (Attribute attribute : input.getAttributes()) {
            output.addAttribute(new Attribute(attribute));
        }
        op.setOutput(output);
        totalCost += output.getTupleCount();
    }

    public void visit(Project op) {
        Relation input = op.getInput().getOutput();
        Relation output = new Relation(input.getTupleCount());

        for (Attribute attribute : op.getAttributes()) {
            for (Attribute attribute2 : input.getAttributes()) {
                if (attribute.equals(attribute2)) {
                    output.addAttribute(new Attribute(attribute2));
                }
            }
        }
        op.setOutput(output);
        totalCost += output.getTupleCount();
    }

    public void visit(Select op) {
        Relation input = op.getInput().getOutput();
        Predicate predicate = op.getPredicate();
        Relation output;

        if (predicate.equalsValue()) {
            output = new Relation(input.getTupleCount() / input.getAttribute(predicate.getLeftAttribute()).getValueCount());
            for (Attribute attribute : input.getAttributes()) {
                int n;
                if (attribute.equals(predicate.getLeftAttribute())) {
                    n = 1;
                } else {
                    n = attribute.getValueCount();
                }
                output.addAttribute(new Attribute(attribute.getName(), n));
            }
        } else {
            int left = input.getAttribute(predicate.getLeftAttribute()).getValueCount();
            int right = input.getAttribute(predicate.getRightAttribute()).getValueCount();
            output = new Relation(input.getTupleCount() / Math.max(left, right));
            for (Attribute attribute : input.getAttributes()) {
                int n;
                if (attribute.equals(predicate.getLeftAttribute()) || attribute.equals(predicate.getRightAttribute())) {
                    n = Math.min(left, right);
                } else {
                    n = attribute.getValueCount();
                }
                output.addAttribute(new Attribute(attribute.getName(), n));
            }
        }
        op.setOutput(output);
        totalCost += output.getTupleCount();
    }

    public void visit(Product op) {
        Relation leftInput = op.getLeft().getOutput();
        Relation rightInput = op.getRight().getOutput();

        Relation output = new Relation(leftInput.getTupleCount() * rightInput.getTupleCount());
        for (Attribute attribute : leftInput.getAttributes()) {
            output.addAttribute(new Attribute(attribute));
        }
        for (Attribute attribute : rightInput.getAttributes()) {
            output.addAttribute(new Attribute(attribute));
        }
        op.setOutput(output);
        totalCost += output.getTupleCount();
    }

    public void visit(Join op) {
        Relation leftInput = op.getLeft().getOutput();
        Relation rightInput = op.getRight().getOutput();
        Predicate predicate = op.getPredicate();

        int left = 0, right = 0;
        for (Attribute attribute : leftInput.getAttributes()) {
            if (attribute.equals(predicate.getLeftAttribute())) {
                left = attribute.getValueCount();
            } else if (attribute.equals(predicate.getRightAttribute())) {
                right = attribute.getValueCount();
            }
        }
        for (Attribute attribute : rightInput.getAttributes()) {
            if (attribute.equals(predicate.getLeftAttribute())) {
                left = attribute.getValueCount();
            } else if (attribute.equals(predicate.getRightAttribute())) {
                right = attribute.getValueCount();
            }
        }

        Relation output = new Relation(leftInput.getTupleCount() * rightInput.getTupleCount() / Math.max(left, right));
        for (Attribute attribute : leftInput.getAttributes()) {
            int n;
            if (attribute.equals(predicate.getLeftAttribute()) || attribute.equals(predicate.getRightAttribute())) {
                n = Math.min(left, right);
            } else {
                n = attribute.getValueCount();
            }
            output.addAttribute(new Attribute(attribute.getName(), n));
        }

        for (Attribute attribute : rightInput.getAttributes()) {
            int n;
            if (attribute.equals(predicate.getLeftAttribute()) || attribute.equals(predicate.getRightAttribute())) {
                n = Math.min(left, right);
            } else {
                n = attribute.getValueCount();
            }
            output.addAttribute(new Attribute(attribute.getName(), n));
        }
        op.setOutput(output);
        totalCost += output.getTupleCount();
    }

    public int getCost(Operator plan) {
        this.totalCost = 0;
        plan.accept(this);
        return this.totalCost;
    }
}
