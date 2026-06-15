package com.human.common.gameplay.entity.living.human.ai;

import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.just.ai.goap.condition.expression.Expression;
import com.just.ai.goap.condition.expression.Expressions;

public class HumanGOAPExpressions {

    public static class ArmorSetTarget {

        private static final Expression<com.human.common.gameplay.entity.living.human.ai.model.ArmorSetTarget> IS_EMPTY = Expressions
            .of("is empty", com.human.common.gameplay.entity.living.human.ai.model.ArmorSetTarget::isEmpty);

        private static final Expression<com.human.common.gameplay.entity.living.human.ai.model.ArmorSetTarget> IS_NOT_EMPTY =
            IS_EMPTY.negate("is not empty");

        public static Expression<com.human.common.gameplay.entity.living.human.ai.model.ArmorSetTarget> isEmpty() {
            return IS_EMPTY;
        }

        public static Expression<com.human.common.gameplay.entity.living.human.ai.model.ArmorSetTarget> isNotEmpty() {
            return IS_NOT_EMPTY;
        }

        public static Expression<com.human.common.gameplay.entity.living.human.ai.model.ArmorSetTarget> allNoneOr(
            ItemTarget.Location location
        ) {
            return Expressions.of(ItemTarget.None.INSTANCE + " or " + location, armorSetTarget -> armorSetTarget.allNoneOrMatch(location));
        }
    }
}
