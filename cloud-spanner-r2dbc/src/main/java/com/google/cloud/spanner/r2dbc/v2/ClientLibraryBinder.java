/*
 * Copyright 2019-2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner.r2dbc.v2;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Value;
import com.google.cloud.spanner.r2dbc.BindingFailureException;
import com.google.cloud.spanner.r2dbc.statement.TypedNull;
import com.google.cloud.spanner.r2dbc.util.Assert;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class ClientLibraryBinder {
  private static final List<ClientLibraryTypeBinder> binders = buildBinders();

  private static List<ClientLibraryTypeBinder> buildBinders() {
    List<ClientLibraryTypeBinder> binders = new ArrayList<>();
    binders.add(
        new ClientLibraryTypeBinderImpl<>(Integer.class,
            (binder, val) -> binder.to(longFromInteger(val))));
    binders.add(new ClientLibraryTypeBinderImpl<>(Long.class, (binder, val) -> binder.to(val)));
    binders.add(new ClientLibraryTypeBinderImpl<>(Double.class, (binder, val) -> binder.to(val)));
    binders.add(new ClientLibraryTypeBinderImpl<>(Boolean.class, (binder, val) -> binder.to(val)));
    binders.add(
        new ClientLibraryTypeBinderImpl<>(ByteArray.class, (binder, val) -> binder.to(val)));
    binders.add(new ClientLibraryTypeBinderImpl<>(Date.class, (binder, val) -> binder.to(val)));
    binders.add(new ClientLibraryTypeBinderImpl<>(String.class, (binder, val) -> binder.to(val)));
    binders.add(
        new ClientLibraryTypeBinderImpl<>(Timestamp.class, (binder, val) -> binder.to(val)));
    binders.add(
        new ClientLibraryTypeBinderImpl<>(BigDecimal.class, (binder, val) -> binder.to(val)));

    binders.add(
        new ClientLibraryTypeBinderImpl<>(
            JsonWrapper.class,
            (binder, val) -> binder.to(val == null ? Value.json(null) : val.getJsonVal())));

    // There is technically one more supported type -  binder.to(Type type, @Nullable Struct value),
    // but it is not clear how r2dbc could pass both the type and the value

    return binders;
  }

  static void bind(Statement.Builder builder, String name, Object value) {
    Assert.requireNonNull(name, "Column name must not be null");
    Assert.requireNonNull(value, "Value must not be null");

    Class<?> valueClass = isTypedNull(value) ? ((TypedNull) value).getType() : value.getClass();

    Optional<ClientLibraryTypeBinder> optionalBinder =
        binders.stream().filter(e -> e.canBind(valueClass)).findFirst();
    if (!optionalBinder.isPresent()) {
      throw new BindingFailureException("Can't find a binder for type: " + valueClass);
    }
    if (!isTypedNull(value)) {
      optionalBinder.get().bind(builder, name, value);
    } else {
      optionalBinder.get().bind(builder, name, null);
    }

  }

  private static boolean isTypedNull(Object value) {
    return value.getClass().equals(TypedNull.class);
  }

  private static Long longFromInteger(Integer intValue) {
    return intValue == null ? null : intValue.longValue();
  }
}
