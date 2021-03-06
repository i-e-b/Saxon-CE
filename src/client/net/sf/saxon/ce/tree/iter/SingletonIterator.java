package client.net.sf.saxon.ce.tree.iter;

import client.net.sf.saxon.ce.om.Item;
import client.net.sf.saxon.ce.om.Sequence;


/**
* SingletonIterator: an iterator over a sequence of zero or one values
*/

public class SingletonIterator implements UnfailingIterator, GroundedIterator {

    private Item item;
    private int position = 0;

    /**
     * Private constructor: external classes should use the factory method
     * @param value the item to iterate over
     */

    private SingletonIterator(Item value) {
        this.item = value;
    }

   /**
    * Factory method.
    * @param item the item to iterate over
    * @return a SingletonIterator over the supplied item, or an EmptyIterator
    * if the supplied item is null.
    */

    public static UnfailingIterator makeIterator(Item item) {
       if (item==null) {
           return EmptyIterator.getInstance();
       } else {
           return new SingletonIterator(item);
       }
   }

    public Item next() {
        if (position == 0) {
            position = 1;
            return item;
        } else if (position == 1) {
            position = -1;
            return null;
        } else {
            return null;
        }
    }

    public int getLastPosition() {
        return 1;
    }

    public UnfailingIterator getAnother() {
        return new SingletonIterator(item);
    }

    public Item getValue() {
        return item;
    }

    /**
     * Return a Value containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding Value. If the value is a closure or a function call package, it will be
     * evaluated and expanded.
     */

    public Sequence materialize() {
        return item;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
