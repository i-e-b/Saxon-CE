package client.net.sf.saxon.ce.expr;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.SequenceIterator;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.*;
import client.net.sf.saxon.ce.value.*;

/**
* A ItemChecker implements the item type checking of "treat as": that is,
* it returns the supplied sequence, checking that all its items are of the correct type
*/

public final class ItemChecker extends UnaryExpression {

    private ItemType requiredItemType;
    private RoleLocator role;

    /**
     * Constructor
     * @param sequence the expression whose value we are checking
     * @param itemType the required type of the items in the sequence
     * @param role information used in constructing an error message
    */

    public ItemChecker(Expression sequence, ItemType itemType, RoleLocator role) {
        super(sequence);
        requiredItemType = itemType;
        this.role = role;
        adoptChildExpression(sequence);
    }

    /**
     * Get the required type
     * @return the required type of the items in the sequence
     */

    public ItemType getRequiredType() {
        return requiredItemType;
    }

    /**
    * Simplify an expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        if (requiredItemType instanceof AnyItemType) {
            return operand;
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        // When typeCheck is called a second time, we might have more information...

        final TypeHierarchy th = TypeHierarchy.getInstance();
        int card = operand.getCardinality();
        if (card == StaticProperty.EMPTY) {
            //value is always empty, so no item checking needed
            return operand;
        }
        ItemType supplied = operand.getItemType();
        int relation = th.relationship(requiredItemType, supplied);
        if (relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMES) {
            return operand;
        } else if (relation == TypeHierarchy.DISJOINT) {
            if (Cardinality.allowsZero(card)) {
                //String message = role.composeErrorMessage(
                //        requiredItemType, operand.getItemType());
                //visitor.getStaticContext().issueWarning("The only value that can pass type-checking is an empty sequence. " +
                //        message, getSourceLocator());
            } else if (requiredItemType.equals(AtomicType.STRING) && th.isSubType(supplied, AtomicType.ANY_URI)) {
                // URI promotion will take care of this at run-time
                return operand;
            } else {
                String message = role.composeErrorMessage(requiredItemType, operand.getItemType());
                typeError(message, role.getErrorCode());
            }
        }
        return this;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        int m = ITERATE_METHOD;
        if (!Cardinality.allowsMany(getCardinality())) {
            m |= EVALUATE_METHOD;
        }
        return m;
    }


    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        return new ItemMappingIterator(base, getMappingFunction(context), true);
    }

    /**
     * Get the mapping function used to implement this item check. This mapping function is applied
     * to each item in the input sequence.
     * @param context The dynamic context used to evaluate the mapping function
     * @return the mapping function. This will be an identity mapping: the output sequence is the same
     * as the input sequence, unless the dynamic type checking reveals an error.
     */

    public ItemMappingFunction getMappingFunction(XPathContext context) {
        ItemCheckMappingFunction map = new ItemCheckMappingFunction();
        map.externalContext = context;
        return map;
    }

    /**
     * Mapping function. This is an identity mapping: either the input items are returned unchanged,
     * or an error is thrown
    */
    private class ItemCheckMappingFunction implements ItemMappingFunction {
        public XPathContext externalContext;
        public Item mapItem(Item item) throws XPathException {
            testConformance(item, externalContext);
            return item;
        }
    }

    /**
    * Evaluate as an Item.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item==null) return null;
        testConformance(item, context);
        return item;
    }


    private void testConformance(Item item, XPathContext context) throws XPathException {
        if (item instanceof AnyURIValue) {
            // Static type checking does not actively convert URIs to Strings, so we
            // have to treat a URI as OK when a string is acceptable.
            item = StringValue.EMPTY_STRING;
        }
        if (!requiredItemType.matchesItem(item)) {
            String message;
            if (context == null) {
                message = "Supplied value of type " + Type.displayTypeName(item) +
                        " does not match the required type of " + role.getMessage();
            } else {
                message = role.composeErrorMessage(requiredItemType, SequenceTool.getItemType(item));
            }
            String errorCode = role.getErrorCode();
            if ("XPDY0050".equals(errorCode)) {
                // error in "treat as" assertion
                dynamicError(message, errorCode);
            } else {
                typeError(message, errorCode);
            }
        }
    }

    /**
     * Determine the data type of the items returned by the expression
     */

	public ItemType getItemType() {
        return requiredItemType;
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                requiredItemType == ((ItemChecker)other).requiredItemType;
    }

    /**
     * get HashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
     */

    @Override
    public int hashCode() {
        return super.hashCode() ^ requiredItemType.hashCode();
    }

}



// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.