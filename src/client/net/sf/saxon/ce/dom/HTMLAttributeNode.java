package client.net.sf.saxon.ce.dom;

import client.net.sf.saxon.ce.event.Receiver;
import client.net.sf.saxon.ce.om.*;
import client.net.sf.saxon.ce.pattern.AnyNodeTest;
import client.net.sf.saxon.ce.pattern.NodeTest;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.iter.*;
import client.net.sf.saxon.ce.tree.util.FastStringBuffer;
import client.net.sf.saxon.ce.tree.util.Navigator;
import client.net.sf.saxon.ce.type.Type;
import client.net.sf.saxon.ce.value.AbstractNode;
import client.net.sf.saxon.ce.value.AtomicValue;
import client.net.sf.saxon.ce.value.UntypedAtomicValue;

/**
 * An attribute node in the DOM - may be XML or HTML
 */
public class HTMLAttributeNode extends AbstractNode implements NodeInfo {

    private HTMLNodeWrapper element;
    private String name;
    private String value;
    private StructuredQName qname;
    private String uri;
    private String prefix;

    public HTMLAttributeNode(HTMLNodeWrapper element, String name, String prefix, String uri, String value) {
        this.element = element;
        this.name = name;
        this.prefix = prefix;
        this.uri = uri;
        this.value = value;
        this.qname = new StructuredQName(prefix, uri, name);
    }

    public int getNodeKind() {
        return Type.ATTRIBUTE;
    }

    public boolean isSameNodeInfo(NodeInfo other) {
        return other instanceof HTMLAttributeNode &&
                element.isSameNodeInfo(((HTMLAttributeNode)other).element) &&
                name.equals(((HTMLAttributeNode)other).name);
    }

    public String getSystemId() {
        return element.getSystemId();
    }

    public String getBaseURI() {
        return element.getBaseURI();
    }

    public int compareOrder(NodeInfo other) {
        if (other instanceof HTMLAttributeNode) {
            if (element.isSameNodeInfo(((HTMLAttributeNode)other).element)) {
                return qname.compareTo(((HTMLAttributeNode)other).qname);
            } else {
                return element.compareOrder(((HTMLAttributeNode)other).element);
            }
        }
        if (other.isSameNodeInfo(element)) {
            return +1;
        }
        return element.compareOrder(other);
    }

    public String getStringValue() {
        return value;
    }

    public StructuredQName getNodeName() {
        return qname;
    }

    public String getLocalPart() {
        return name;
    }

    public String getURI() {
        return uri;
    }

    public String getDisplayName() {
        return (prefix.length() == 0)? name : prefix + ':' + name;
    }

    public NodeInfo getParent() {
        return element;
    }

    private UnfailingIterator iterateAxis0(byte axisNumber) {
        switch (axisNumber) {
            case Axis.ANCESTOR:
                return element.iterateAxis(Axis.ANCESTOR_OR_SELF, AnyNodeTest.getInstance());

            case Axis.ANCESTOR_OR_SELF:
                return new PrependIterator(this, element.iterateAxis(Axis.ANCESTOR_OR_SELF, AnyNodeTest.getInstance()));

            case Axis.ATTRIBUTE:
                return EmptyIterator.getInstance();

            case Axis.CHILD:
                return EmptyIterator.getInstance();

            case Axis.DESCENDANT:
                return EmptyIterator.getInstance();

            case Axis.DESCENDANT_OR_SELF:
                return SingletonIterator.makeIterator(this);

            case Axis.FOLLOWING:
                return new Navigator.FollowingEnumeration(this);

            case Axis.FOLLOWING_SIBLING:
                return EmptyIterator.getInstance();

            case Axis.NAMESPACE:
                return EmptyIterator.getInstance();

            case Axis.PARENT:
                return SingletonIterator.makeIterator(element);

            case Axis.PRECEDING:
                return new Navigator.PrecedingEnumeration(this, false);

            case Axis.PRECEDING_SIBLING:
                return EmptyIterator.getInstance();

            case Axis.SELF:
                return SingletonIterator.makeIterator(this);

            default:
                throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    public UnfailingIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {

        return Navigator.newAxisFilter(iterateAxis0(axisNumber), nodeTest);
    }

    public NodeInfo getRoot() {
        return element.getRoot();
    }

    public DocumentInfo getDocumentRoot() {
        return element.getDocumentRoot();
    }

    public boolean hasChildNodes() {
        return false;
    }

    public void generateId(FastStringBuffer buffer) {
        element.generateId(buffer);
        buffer.append(""+name.hashCode());
    }

    public int getDocumentNumber() {
        return element.getDocumentNumber();
    }

    public void copy(Receiver out, int copyOptions) throws XPathException {
        out.attribute(qname, value);
    }

    public NamespaceBinding[] getDeclaredNamespaces(NamespaceBinding[] buffer) {
        return NamespaceBinding.EMPTY_ARRAY;
    }

    public AtomicValue getTypedValue() {
        return new UntypedAtomicValue(value);
    }

    /**
     * Get the index position of this node among its siblings (starting from 0)
     *
     * @return 0 for the first child, 1 for the second child, etc.
     */
    public int getSiblingPosition() {
        return 1;
    }
}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.


