package org.openhab.binding.xsense.internal.api.communication;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Subscription {
    private String topic = "";
    private Class<?> dataClass = null;

    public Subscription(Class<?> dataClass, String topic) {
        this.dataClass = dataClass;
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    public Class<?> getDataClass() {
        return dataClass;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
                append(topic).append(dataClass.toString()).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Subscription))
            return false;
        if (obj == this)
            return true;

        Subscription rhs = (Subscription) obj;
        return new EqualsBuilder().append(topic, rhs.topic).append(dataClass, rhs.dataClass).isEquals();
    }
}
