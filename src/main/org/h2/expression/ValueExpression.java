/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression;

import org.h2.engine.Session;
import org.h2.expression.condition.Comparison;
import org.h2.index.IndexCondition;
import org.h2.message.DbException;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.TypeInfo;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueCollectionBase;
import org.h2.value.ValueNull;
import org.h2.value.ValueResultSet;

/**
 * An expression representing a constant value.
 */
public class ValueExpression extends Expression {

    /**
     * The expression represents ValueNull.INSTANCE.
     */
    public static final ValueExpression NULL = new ValueExpression(ValueNull.INSTANCE);

    /**
     * This special expression represents the default value. It is used for
     * UPDATE statements of the form SET COLUMN = DEFAULT. The value is
     * ValueNull.INSTANCE, but should never be accessed.
     */
    public static final ValueExpression DEFAULT = new ValueExpression(ValueNull.INSTANCE);

    /**
     * The expression represents ValueBoolean.TRUE.
     */
    public static final ValueExpression TRUE = new ValueExpression(ValueBoolean.TRUE);

    /**
     * The expression represents ValueBoolean.FALSE.
     */
    public static final ValueExpression FALSE = new ValueExpression(ValueBoolean.FALSE);

    /**
     * The value.
     */
    final Value value;

    ValueExpression(Value value) {
        this.value = value;
    }

    /**
     * Create a new expression with the given value.
     *
     * @param value the value
     * @return the expression
     */
    public static ValueExpression get(Value value) {
        if (value == ValueNull.INSTANCE) {
            return NULL;
        }
        if (value.getValueType() == Value.BOOLEAN) {
            return getBoolean(value.getBoolean());
        }
        return new ValueExpression(value);
    }

    /**
     * Create a new expression with the given boolean value.
     *
     * @param value the boolean value
     * @return the expression
     */
    public static ValueExpression getBoolean(Value value) {
        if (value == ValueNull.INSTANCE) {
            return TypedValueExpression.UNKNOWN;
        }
        return getBoolean(value.getBoolean());
    }

    /**
     * Create a new expression with the given boolean value.
     *
     * @param value the boolean value
     * @return the expression
     */
    public static ValueExpression getBoolean(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public Value getValue(Session session) {
        return value;
    }

    @Override
    public TypeInfo getType() {
        return value.getType();
    }

    @Override
    public void createIndexConditions(Session session, TableFilter filter) {
        if (value.getValueType() == Value.BOOLEAN && !value.getBoolean()) {
            filter.addIndexCondition(IndexCondition.get(Comparison.FALSE, null, this));
        }
    }

    @Override
    public Expression getNotIfPossible(Session session) {
        return new Comparison(session, Comparison.EQUAL, this, ValueExpression.FALSE);
    }

    @Override
    public void mapColumns(ColumnResolver resolver, int level, int state) {
        // nothing to do
    }

    @Override
    public Expression optimize(Session session) {
        return this;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public boolean isNullConstant() {
        return this == NULL;
    }

    @Override
    public boolean isValueSet() {
        return true;
    }

    @Override
    public void setEvaluatable(TableFilter tableFilter, boolean b) {
        // nothing to do
    }

    @Override
    public StringBuilder getSQL(StringBuilder builder, boolean alwaysQuote) {
        if (this == DEFAULT) {
            builder.append("DEFAULT");
        } else {
            value.getSQL(builder);
        }
        return builder;
    }

    @Override
    public void updateAggregate(Session session, int stage) {
        // nothing to do
    }

    @Override
    public boolean isEverything(ExpressionVisitor visitor) {
        switch (visitor.getType()) {
        case ExpressionVisitor.OPTIMIZABLE_AGGREGATE:
        case ExpressionVisitor.DETERMINISTIC:
        case ExpressionVisitor.READONLY:
        case ExpressionVisitor.INDEPENDENT:
        case ExpressionVisitor.EVALUATABLE:
        case ExpressionVisitor.SET_MAX_DATA_MODIFICATION_ID:
        case ExpressionVisitor.NOT_FROM_RESOLVER:
        case ExpressionVisitor.GET_DEPENDENCIES:
        case ExpressionVisitor.QUERY_COMPARABLE:
        case ExpressionVisitor.GET_COLUMNS1:
        case ExpressionVisitor.GET_COLUMNS2:
            return true;
        default:
            throw DbException.throwInternalError("type=" + visitor.getType());
        }
    }

    @Override
    public int getCost() {
        return 0;
    }

    @Override
    public Expression[] getExpressionColumns(Session session) {
        int valueType = getType().getValueType();
        switch (valueType) {
        case Value.ARRAY:
        case Value.ROW:
            return getExpressionColumns(session, (ValueCollectionBase) getValue(session));
        case Value.RESULT_SET:
            return getExpressionColumns(session, ((ValueResultSet) getValue(session)).getResult());
        }
        return super.getExpressionColumns(session);
    }
}
