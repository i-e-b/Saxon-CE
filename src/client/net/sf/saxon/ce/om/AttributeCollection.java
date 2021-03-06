package client.net.sf.saxon.ce.om;


/**
 * AttributeCollection represents the collection of attributes available on a particular element
 * node. It is modelled on the SAX2 Attributes interface, but is extended firstly to work with
 * Saxon NamePools, and secondly to provide type information as required by the XPath 2.0 data model.
 */

public class AttributeCollection {

    // Attribute values are maintained as an array of Strings.

    private String[] values = null;
    private StructuredQName[] names = null;
    private int used = 0;

    // Empty attribute collection. The caller is trusted not to try and modify it.

    public static AttributeCollection EMPTY_ATTRIBUTE_COLLECTION =
            new AttributeCollection();

    /**
     * Create an empty attribute list.
     */

    public AttributeCollection() {
        used = 0;
    }

    /**
     * Add an attribute to an attribute list. The parameters correspond
     * to the parameters of the {@link client.net.sf.saxon.ce.event.Receiver#attribute(StructuredQName, CharSequence)}
     * method. There is no check that the name of the attribute is distinct from other attributes
     * already in the collection: this check must be made by the caller.
     *
     * @param nameCode Integer representing the attribute name.
     * @param value    The attribute value (must not be null)
     */

    public void addAttribute(StructuredQName nameCode, String value) {
        if (values == null) {
            values = new String[5];
            names = new StructuredQName [5];
            used = 0;
        }
        if (values.length == used) {
            int newsize = (used == 0 ? 5 : used * 2);
            String[] v2 = new String[newsize];
            StructuredQName[] c2 = new StructuredQName[newsize];
            System.arraycopy(values, 0, v2, 0, used);
            System.arraycopy(names, 0, c2, 0, used);
            values = v2;
            names = c2;
        }
        names[used] = nameCode;
        values[used++] = value;
    }

    /**
     * Clear the attribute list. This removes the values but doesn't free the memory used.
     * free the memory, use clear() then compact().
     */

    public void clear() {
        used = 0;
    }

    /**
     * Compact the attribute list to avoid wasting memory
     */

    public void compact() {
        if (used == 0) {
            names = null;
            values = null;
        } else if (values.length > used) {
            String[] v2 = new String[used];
            StructuredQName[] c2 = new StructuredQName[used];
            System.arraycopy(values, 0, v2, 0, used);
            System.arraycopy(names, 0, c2, 0, used);
            values = v2;
            names = c2;
        }
    }

    /**
     * Return the number of attributes in the list.
     *
     * @return The number of attributes that have been created in this attribute collection. This is the number
     * of slots used in the list, including any slots allocated to attributes that have since been deleted.
     * Such slots are not reused, to preserve attribute identity.
     */

    public int getLength() {
        return (values == null ? 0 : used);
    }

    /**
     * Get the name of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The  name of the attribute as a StructuredQName, or null if there
     *         is no attribute at that position.
     */

    public StructuredQName getStructuredQName(int index) {
        if (names == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }

        return names[index];
    }

    /**
     * Get the prefix of the name of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The prefix of the attribute name as a string, or null if there
     *         is no attribute at that position. Returns "" for an attribute that
     *         has no prefix.
     */

    public String getPrefix(int index) {
        if (names == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return names[index].getPrefix();
    }

    /**
     * Get the local name of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The local name of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public String getLocalName(int index) {
        if (names == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return names[index].getLocalName();
    }

    /**
     * Get the namespace URI of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The local name of the attribute as a string, or null if there
     *         is no attribute at that position.
     */

    public String getURI(int index) {
        if (names == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return names[index].getNamespaceURI();
    }


    /**
     * Get the value of an attribute (by position).
     *
     * @param index The position of the attribute in the list.
     * @return The attribute value as a string, or null if
     *         there is no attribute at that position.
     */

    public String getValue(int index) {
        if (values == null) {
            return null;
        }
        if (index < 0 || index >= used) {
            return null;
        }
        return values[index];
    }

    /**
     * Get the value of an attribute (by name).
     *
     * @param uri       The namespace uri of the attribute.
     * @param localname The local name of the attribute.
     * @return The index position of the attribute
     */

    public String getValue(String uri, String localname) {
        if (names == null) {
            return null;
        }
        for (int i = 0; i < used; i++) {
            if (names[i].getNamespaceURI().equals(uri) && names[i].getLocalName().equals(localname)) {
                return values[i];
            }
        }
        return null;
    }

    /**
     * Find an attribute by structured QName
     * @param name the fingerprint representing the name of the required attribute
     * @return the index of the attribute, or -1 if absent
     */

    public int findByStructuredQName(StructuredQName name) {
        if (names == null) {
            return -1;
        }
        for (int i = 0; i < used; i++) {
            if (names[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Determine whether a given attribute has the is-ID property set
     */

    public boolean isId(int index) {
        return StructuredQName.XML_ID.equals(names[index]);
    }


}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
