package client.net.sf.saxon.ce.type;

import client.net.sf.saxon.ce.om.Item;



/**
 * ItemType is an interface that allows testing of whether an Item conforms to an
 * expected type. ItemType represents the types in the type hierarchy in the XPath model,
 * as distinct from the schema model: an item type is either item() (matches everything),
 * a node type (matches nodes), an atomic type (matches atomic values), or empty()
 * (matches nothing). Atomic types, represented by the class AtomicType, are also
 * instances of SimpleType in the schema type hierarchy. Node Types, represented by
 * the class NodeTest, are also Patterns as used in XSLT.
 *
 * <p>Saxon assumes that apart from {@link AnyItemType} (which corresponds to <code>item()</item>
 * and matches anything), every ItemType will be either a {@link AtomicType}, or a
 *  {@link client.net.sf.saxon.ce.pattern.NodeTest}. User-defined implementations of ItemType must therefore extend one of those
 * three classes/interfaces.</p>
 * @see AtomicType
 * @see client.net.sf.saxon.ce.pattern.NodeTest
*/

public interface ItemType  {

    /**
     * Test whether a given item conforms to this type
     *
     *
     * @param item The item to be tested
      * @return true if the item is an instance of this type; false otherwise
    */

    public boolean matchesItem(Item item);

    /**
     * Get the type from which this item type is derived by restriction. This
     * is the supertype in the XPath type heirarchy, as distinct from the Schema
     * base type: this means that the supertype of xs:boolean is xs:anyAtomicType,
     * whose supertype is item() (rather than xs:anySimpleType).
     * <p>
     * In fact the concept of "supertype" is not really well-defined, because the types
     * form a lattice rather than a hierarchy. The only real requirement on this function
     * is that it returns a type that strictly subsumes this type, ideally as narrowly
     * as possible.
     * @return the supertype, or null if this type is item()
     */

    public ItemType getSuperType();

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized
     * @return  the item type of the atomic values that will be produced when an item
     * of this type is atomized
     */

    public AtomicType getAtomizedItemType();

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
