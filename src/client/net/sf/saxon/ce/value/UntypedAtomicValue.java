package client.net.sf.saxon.ce.value;

import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.type.ConversionResult;
import client.net.sf.saxon.ce.type.StringToDouble;
import client.net.sf.saxon.ce.type.ValidationFailure;

/**
* An Untyped Atomic value. This inherits from StringValue for implementation convenience, even
* though an untypedAtomic value is not a String in the data model type hierarchy.
*/

public class UntypedAtomicValue extends StringValue {

    // If the value is used once as a number, it's likely that it will be used
    // repeatedly as a number, so we cache the result of conversion

    DoubleValue doubleValue = null;

    /**
    * Constructor
    * @param value the String value. Null is taken as equivalent to "".
    */

    public UntypedAtomicValue(CharSequence value) {
        this.value = (value==null ? "" : value);
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public AtomicType getItemType() {
        return AtomicType.UNTYPED_ATOMIC;
    }

    /**
     * Convert a value to another primitive data type, with control over how validation is
     * handled.
     *
     * @param requiredType type code of the required atomic type. This must not be a namespace-sensitive type.
     * @return the result of the conversion, if successful. If unsuccessful, the value returned
     * will be a ValidationErrorValue. The caller must check for this condition. No exception is thrown, instead
     * the exception will be encapsulated within the ErrorValue.
     */

    public ConversionResult convert(AtomicType requiredType) {
        if (requiredType == AtomicType.STRING) {
            if (value.length() == 0) {
                // this case is common!
                return StringValue.EMPTY_STRING;
            } else {
                return new StringValue(value);
            }
        } else if (requiredType == AtomicType.UNTYPED_ATOMIC) {
            return this;
        } else if (requiredType == AtomicType.DOUBLE || requiredType == AtomicType.NUMERIC) {
            // for conversion to double (common in 1.0 mode), cache the result
            try {
                return toDouble();
            } catch (XPathException e) {
                return new ValidationFailure(e.getMessage());
            }
        } else {
            return super.convert(requiredType);
        }
    }

    /**
     * Convert the value to a double, returning a DoubleValue
     */

    private AtomicValue toDouble() throws XPathException {
        if (doubleValue == null) {
            try {
                double d = StringToDouble.stringToNumber(value);
                doubleValue = new DoubleValue(d);
            } catch (NumberFormatException e) {
                throw new XPathException("Cannot convert string " + Err.wrap(value) + " to a double");
            }
        }
        return doubleValue;
    }

    /**
    * Compare an untypedAtomic value with another value, using a given collator to perform
    * any string comparisons. This works by converting the untypedAtomic value to the type
     * of the other operand, which is the correct behavior for operators like "=" and "!=",
     * but not for "eq" and "ne": in the latter case, the untypedAtomic value is converted
     * to a string and this method is therefore not used.
     * @return -1 if the this value is less than the other, 0 if they are equal, +1 if this
     * value is greater.
     * @throws ClassCastException if the value cannot be cast to the type of the other operand
    */

    public int compareTo(AtomicValue other, StringCollator collator) {
        if (other instanceof NumericValue) {
            if (doubleValue == null) {
                try {
                    doubleValue = (DoubleValue) convert(AtomicType.DOUBLE
                    ).asAtomic();
                } catch (XPathException err) {
                    throw new ClassCastException("Cannot convert untyped value " +
                        '\"' + getStringValue() + "\" to a double");
                }
            }
            return doubleValue.compareTo(other);
        } else if (other instanceof StringValue) {
            return collator.compareStrings(getStringValue(), other.getStringValue());
        } else {
            ConversionResult result = convert(other.getItemType());
            if (result instanceof ValidationFailure) {
                throw new ClassCastException("Cannot convert untyped atomic value '" + getStringValue()
                        + "' to type " + other.getItemType());
            }
            return ((Comparable) result).compareTo(other);

        } 
    }

    /**
     * Convert to Java object (for passing to external functions)
     */

//    public Object convertAtomicToJava(Class target, XPathContext context) throws XPathException {
//        if (target == Object.class) {
//            return getStringValue();
//        } else if (target.isAssignableFrom(StringValue.class)) {
//            return this;
//        } else if (target == String.class || target == CharSequence.class) {
//            return getStringValue();
//        } else if (target == boolean.class) {
//            BooleanValue bval = (BooleanValue)convertPrimitive(BuiltInAtomicType.BOOLEAN, true, context).asAtomic();
//            return Boolean.valueOf(bval.getBooleanValue());
//        } else if (target == Boolean.class) {
//            BooleanValue bval = (BooleanValue)convertPrimitive(BuiltInAtomicType.BOOLEAN, true, context).asAtomic();
//            return Boolean.valueOf(bval.getBooleanValue());
//        } else if (target == double.class) {
//            DoubleValue dval = (DoubleValue)convertPrimitive(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
//            return new Double(dval.getDoubleValue());
//        } else if (target == Double.class) {
//            DoubleValue dval = (DoubleValue)convertPrimitive(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
//            return new Double(dval.getDoubleValue());
//        } else if (target == float.class) {
//            DoubleValue dval = (DoubleValue)convertPrimitive(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
//            return new Float(dval.getDoubleValue());
//        } else if (target == Float.class) {
//            DoubleValue dval = (DoubleValue)convertPrimitive(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
//            return new Float(dval.getDoubleValue());
//        } else if (target == long.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Long(dval.longValue());
//        } else if (target == Long.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Long(dval.longValue());
//        } else if (target == int.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Integer((int) dval.longValue());
//        } else if (target == Integer.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Integer((int) dval.longValue());
//        } else if (target == short.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Short((short) dval.longValue());
//        } else if (target == Short.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Short((short) dval.longValue());
//        } else if (target == byte.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Byte((byte) dval.longValue());
//        } else if (target == Byte.class) {
//            Int64Value dval = (Int64Value)convertPrimitive(BuiltInAtomicType.INTEGER, true, context).asAtomic();
//            return new Byte((byte) dval.longValue());
//        } else if (target == char.class || target == Character.class) {
//            if (value.length() == 1) {
//                return new Character(value.charAt(0));
//            } else {
//                XPathException de = new XPathException("Cannot convert xs:string to Java char unless length is 1");
//                de.setXPathContext(context);
//                de.setErrorCode(SaxonErrorCode.SXJE0005);
//                throw de;
//            }
//        } else {
//            Object o = super.convertSequenceToJava(target, context);
//            if (o == null) {
//                XPathException err = new XPathException("Conversion of xs:untypedAtomic to " + target.getName() + " is not supported");
//                err.setXPathContext(context);
//                err.setErrorCode(SaxonErrorCode.SXJE0006);
//                throw err;
//            }
//            return o;
//        }
//    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.

