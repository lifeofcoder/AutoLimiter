package com.lifeofcoder.autolimiter.dashboard.ignite;

import com.lifeofcoder.autolimiter.dashboard.mapping.FieldWrapper;
import com.lifeofcoder.autolimiter.dashboard.mapping.MappingUtil;
import com.lifeofcoder.autolimiter.dashboard.model.BaseIgniteModel;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.configuration.CacheConfiguration;

import java.io.Serializable;
import java.util.*;

/**
 * @author lixiaobin6
 * 下午4:33 2018/7/25
 */
public class IgniteDao<V extends BaseIgniteModel> implements Serializable {
    private Ignite ignite;
    protected IgniteCache<String, V> cache;

    public IgniteDao(Ignite ignite, CacheConfiguration<String, V> cacheConfig) {
        //        Preconditions.checkArgument(ignite != null, "ignite can not be null.");
        //        Preconditions.checkArgument(cacheConfig != null, "cache config can not be null.");

        this.ignite = ignite;
        this.cache = this.ignite.getOrCreateCache(cacheConfig);
    }

    public void addOrUpdate(V t) {
        cache.put(t.key(), t);
    }

    public boolean deleteByKey(String k) {
        return cache.remove(k);
    }

    public V getByKey(String k) {
        return cache.get(k);
    }

    public List<Map<String, Object>> query(SqlFieldsQuery query) {
        FieldsQueryCursor<List<?>> queryCursor = cache.query(query);
        List<Map<String, Object>> result = new ArrayList<>();
        int column = queryCursor.getColumnsCount();
        Iterator it = queryCursor.iterator();
        while (it.hasNext()) {
            Map<String, Object> map = new HashMap<>();
            List<?> data = (List<?>) it.next();
            for (int i = 0; i < column; i++) {
                map.put(queryCursor.getFieldName(i), data.get(i));
            }
            result.add(map);
        }
        return result;
    }

    public <T> List<T> query(String sql, Class<T> cls) {
        return query(new SqlFieldsQuery(sql), cls);
    }

    public <T> List<T> query(SqlFieldsQuery query, Class<T> cls) {
        FieldsQueryCursor<List<?>> queryCursor = cache.query(query);
        List<Map<String, Object>> result = new ArrayList<>();
        int column = queryCursor.getColumnsCount();
        Iterator it = queryCursor.iterator();
        FieldWrapper fieldWrapper = MappingUtil.getOrCreate(cls);

        List<T> objList = new ArrayList<>();
        while (it.hasNext()) {
            List<?> data = (List<?>) it.next();
            T obj = null;
            try {
                obj = cls.newInstance();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < column; i++) {
                fieldWrapper.set(obj, queryCursor.getFieldName(i), data.get(i));
            }
            objList.add(obj);
        }

        return objList;
    }

    public Collection<V> query(Set<String> keys) {
        return cache.getAll(keys).values();
    }

    public void remove(Set<String> keys) {
        cache.removeAll(keys);
    }

    public static class SimpleSqlBuilder {
        private StringBuffer buffer = new StringBuffer("");
        private List args = new ArrayList();
        private Class t;

        public SimpleSqlBuilder(Class t) {
            this.t = t;
        }

        public static SimpleSqlBuilder create(Class t) {
            return new SimpleSqlBuilder(t);
        }

        public SimpleSqlBuilder and(String column, Object value) {
            if (buffer.length() > 0) {
                buffer.append(" and ");
            }

            buffer.append(column).append("=?");
            args.add(value);
            return this;
        }

        public SimpleSqlBuilder in(String column, List list) {
            if (list == null || list.size() <= 0)
                return this;
            if (buffer.length() > 0) {
                buffer.append(" and ");
            }

            buffer.append(column).append(" in (");
            for (int i = 0; i < list.size(); i++) {
                if (i != list.size() - 1) {
                    buffer.append("?,");
                    args.add(list.get(i));
                }
                else {
                    buffer.append("?)");
                    args.add(list.get(i));
                }
            }
            return this;
        }

        public SimpleSqlBuilder or(String column, Object value) {
            if (buffer.length() > 0) {
                buffer.append(" or ");
            }
            buffer.append(column).append("=?");
            args.add(value);
            return this;
        }

        public SimpleSqlBuilder like(String column, Object value) {
            if (buffer.length() > 0) {
                buffer.append(" and ");
            }
            buffer.append(column).append(" like ? ");
            args.add("%" + value + "%");
            return this;
        }

        public SimpleSqlBuilder order(String column) {
            buffer.append(" order by ?");
            args.add(column);
            return this;
        }

        public SqlQuery build() {
            if (buffer.toString().length() < 1) {
                return new SqlQuery(t, "1=1");
            }
            return new SqlQuery(t, buffer.toString()).setArgs(args.toArray());
        }
    }
}
