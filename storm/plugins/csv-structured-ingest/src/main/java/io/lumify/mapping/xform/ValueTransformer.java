package io.lumify.mapping.xform;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * A method for transforming an input string to a particular
 * value type.
 * @param <T> the type of value returned by this transformer
 */
@JsonTypeInfo(include=As.PROPERTY, property="dataType", use=Id.NAME)
@JsonSubTypes({
    @Type(BigDecimalValueTransformer.class),
    @Type(BigIntegerValueTransformer.class),
    @Type(BooleanMappedValueTransformer.class),
    @Type(BooleanValueTransformer.class),
    @Type(DateValueTransformer.class),
    @Type(DoubleValueTransformer.class),
    @Type(IntegerValueTransformer.class),
    @Type(LongValueTransformer.class),
    @Type(ReplacingValueTransformer.class),
    @Type(StringMappedValueTransformer.class),
    @Type(StringValueTransformer.class)
})
public interface ValueTransformer<T> {
    /**
     * Transform the input string to the specified value type.  This
     * method should return <code>null</code> if the value could not
     * be transformed for any reason.
     * @param input the input value
     * @return the transformed value
     */
    T transform(final String input);
}
