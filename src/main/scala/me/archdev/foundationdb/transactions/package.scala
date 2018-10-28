package me.archdev.foundationdb

import cats.data.StateT
import cats.effect.IO
import com.apple.foundationdb.Transaction
import com.apple.foundationdb.tuple.Tuple
import me.archdev.foundationdb.algebra.QueryAlgebra
import me.archdev.foundationdb.Utils._
import me.archdev.foundationdb.serializers._

package object transactions {

  implicit val QueryAlgebraInterpreter: QueryAlgebra[TransactionPlan] =
    new QueryAlgebra[TransactionPlan] {
      override def set[K, V](key: K, value: V)(implicit ks: Tupler[K],
                                               vs: Tupler[V],
                                               subspace: Subspace = Subspace()): TransactionPlan[Unit] =
        StateT { tr =>
          tr.set(subspace.raw.pack(key.toTuple), value.toTuple.pack())
          IO(tr -> ())
        }

      override def get[K, V](
          key: K
      )(implicit ks: Tupler[K], vs: Tupler[V], subspace: Subspace = Subspace()): TransactionPlan[Option[V]] =
        StateT { tr =>
          tr.get(subspace.raw.pack(key.toTuple))
            .toIO
            .map(Option.apply)
            .map(_.map(Tuple.fromBytes).map(_.fromTuple[V]))
            .map(tr -> _)
        }

      override def delete[K](key: K)(implicit ks: Tupler[K], subspace: Subspace = Subspace()): TransactionPlan[Unit] =
        StateT { tr =>
          tr.clear(subspace.raw.pack(key.toTuple))
          IO(tr -> ())
        }

      override def raw[V](f: Transaction => V): TransactionPlan[V] =
        StateT { tr =>
          IO(tr -> f(tr))
        }
    }

}