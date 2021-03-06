package AppliedIntegrations.Network.Packets;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author Azazell
 *
 * Mark field with this annotation, to ignore it on transition process
 * NOT transient
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Overlook { }
