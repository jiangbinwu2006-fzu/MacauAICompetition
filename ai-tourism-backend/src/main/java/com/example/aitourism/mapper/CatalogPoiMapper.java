package com.example.aitourism.mapper;

import com.example.aitourism.entity.CatalogPoi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CatalogPoiMapper {

    @Select("""
            <script>
            SELECT * FROM t_macau_catalog_poi
            WHERE status = 'ACTIVE'
              AND valid_until &gt;= CURRENT_DATE
            <if test='region != null and region != ""'>
              AND region = #{region}
            </if>
            <if test='category != null and category != ""'>
              AND category = #{category}
            </if>
            <if test='keyword != null and keyword != ""'>
              AND (name_zh_hans LIKE CONCAT('%', #{keyword}, '%')
                OR name_zh_hant LIKE CONCAT('%', #{keyword}, '%')
                OR name_en LIKE CONCAT('%', #{keyword}, '%')
                OR name_pt LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY region, category, id
            </script>
            """)
    List<CatalogPoi> findActive(
            @Param("region") String region,
            @Param("category") String category,
            @Param("keyword") String keyword
    );

    @Select("SELECT DISTINCT region FROM t_macau_catalog_poi WHERE status = 'ACTIVE' ORDER BY region")
    List<String> findRegions();

    @Select("SELECT DISTINCT category FROM t_macau_catalog_poi WHERE status = 'ACTIVE' ORDER BY category")
    List<String> findCategories();
}
