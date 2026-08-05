# Java 側テンプレート

`{{Feature}}` = PascalCase 単数（例 `Sector`）
`{{feature}}` = lowerCamel 単数（例 `sector`）
`{{features}}` = URL 複数形（画面は kebab-case、REST は snake_case。1 語なら同一）
`{{table}}` = テーブル名（例 `sectors`）

列は `id, code, name` を例にしている。実テーブルに合わせて増減させる。

---

## 1. Entity

`src/main/java/org/example/web/entity/{{Feature}}Entity.java`

```java
package org.example.web.entity;

import org.seasar.doma.Column;
import org.seasar.doma.Entity;
import org.seasar.doma.GeneratedValue;
import org.seasar.doma.GenerationType;
import org.seasar.doma.Id;
import org.seasar.doma.Table;

@Entity(immutable = false)
@Table(name = "{{table}}")
public class {{Feature}}Entity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "code")
    private String code;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
```

## 2. DAO

`src/main/java/org/example/web/dao/{{Feature}}Dao.java`

```java
package org.example.web.dao;

import java.util.List;
import java.util.Optional;

import org.example.web.entity.{{Feature}}Entity;
import org.seasar.doma.Dao;
import org.seasar.doma.Delete;
import org.seasar.doma.Insert;
import org.seasar.doma.Select;
import org.seasar.doma.Update;
import org.seasar.doma.boot.ConfigAutowireable;

@Dao
@ConfigAutowireable
public interface {{Feature}}Dao {
    @Select
    List<{{Feature}}Entity> selectAll();

    @Select
    Optional<{{Feature}}Entity> selectById(Integer id);

    @Insert
    int insert({{Feature}}Entity entity);

    @Update
    int update({{Feature}}Entity entity);

    @Delete
    int delete({{Feature}}Entity entity);
}
```

## 3. SQL

`src/main/resources/META-INF/org/example/web/dao/{{Feature}}Dao/selectAll.sql`

```sql
select
  /*%expand*/*
from
  {{table}}
order by
  id
```

`.../selectById.sql`

```sql
select
  /*%expand*/*
from
  {{table}}
where
  id = /* id */0
```

## 4. ResponseDto

`src/main/java/org/example/web/stock/{{feature}}/domain/{{Feature}}ResponseDto.java`

```java
package org.example.web.stock.{{feature}}.domain;

public class {{Feature}}ResponseDto {
    private final Integer id;
    private final String code;
    private final String name;

    public {{Feature}}ResponseDto(Integer id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
```

## 5. Form

`src/main/java/org/example/web/stock/{{feature}}/domain/{{Feature}}Form.java`

```java
package org.example.web.stock.{{feature}}.domain;

public record {{Feature}}Form(
        Integer id,
        String code,
        String name) {
}
```

## 6. Service インタフェース

`src/main/java/org/example/web/stock/{{feature}}/service/{{Feature}}Service.java`

```java
package org.example.web.stock.{{feature}}.service;

import java.util.List;

import org.example.web.stock.{{feature}}.domain.{{Feature}}Form;
import org.example.web.stock.{{feature}}.domain.{{Feature}}ResponseDto;

public interface {{Feature}}Service {
    List<{{Feature}}ResponseDto> initialDispAll();

    void insert{{Feature}}Info({{Feature}}Form form);

    void update{{Feature}}Info({{Feature}}Form form);

    void delete{{Feature}}InfoById(Integer id);
}
```

## 7. Service 実装

`src/main/java/org/example/web/stock/{{feature}}/service/{{Feature}}ServiceImpl.java`

```java
package org.example.web.stock.{{feature}}.service;

import java.util.List;

import org.example.web.dao.{{Feature}}Dao;
import org.example.web.entity.{{Feature}}Entity;
import org.example.web.exception.NotFoundException;
import org.example.web.stock.{{feature}}.domain.{{Feature}}Form;
import org.example.web.stock.{{feature}}.domain.{{Feature}}ResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class {{Feature}}ServiceImpl implements {{Feature}}Service {

    private final {{Feature}}Dao {{feature}}Dao;

    public {{Feature}}ServiceImpl({{Feature}}Dao {{feature}}Dao) {
        this.{{feature}}Dao = {{feature}}Dao;
    }

    @Override
    public List<{{Feature}}ResponseDto> initialDispAll() {
        return {{feature}}Dao.selectAll().stream()
                .map(entity -> new {{Feature}}ResponseDto(
                        entity.getId(),
                        entity.getCode(),
                        entity.getName()))
                .toList();
    }

    @Override
    public void insert{{Feature}}Info({{Feature}}Form form) {
        {{Feature}}Entity entity = new {{Feature}}Entity();
        entity.setId(null);
        entity.setCode(form.code());
        entity.setName(form.name());

        {{feature}}Dao.insert(entity);
    }

    @Override
    public void update{{Feature}}Info({{Feature}}Form form) {
        {{Feature}}Entity entity = {{feature}}Dao.selectById(form.id())
                .orElseThrow(() -> new NotFoundException("{{Feature}} が見つかりません。"));

        entity.setCode(form.code());
        entity.setName(form.name());

        {{feature}}Dao.update(entity);
    }

    @Override
    public void delete{{Feature}}InfoById(Integer id) {
        {{Feature}}Entity entity = {{feature}}Dao.selectById(id)
                .orElseThrow(() -> new NotFoundException("{{Feature}} が見つかりません。"));

        {{feature}}Dao.delete(entity);
    }
}
```

## 8. 画面 Controller

`src/main/java/org/example/web/stock/{{feature}}/controller/{{Feature}}Controller.java`

```java
package org.example.web.stock.{{feature}}.controller;

import java.util.List;

import org.example.web.stock.{{feature}}.domain.{{Feature}}ResponseDto;
import org.example.web.stock.{{feature}}.service.{{Feature}}Service;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/{{features}}")
public class {{Feature}}Controller {

    private final {{Feature}}Service {{feature}}Service;

    public {{Feature}}Controller({{Feature}}Service {{feature}}Service) {
        this.{{feature}}Service = {{feature}}Service;
    }

    @GetMapping("")
    public ModelAndView display(ModelAndView mav) {
        mav.setViewName("{{feature}}-list/{{feature}}-list");
        List<{{Feature}}ResponseDto> items = {{feature}}Service.initialDispAll();
        mav.addObject("items", items);
        return mav;
    }
}
```

## 9. REST Controller

`src/main/java/org/example/web/stock/{{feature}}/controller/{{Feature}}RestController.java`

```java
package org.example.web.stock.{{feature}}.controller;

import org.example.web.stock.{{feature}}.domain.{{Feature}}Form;
import org.example.web.stock.{{feature}}.service.{{Feature}}Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest_{{features}}")
public class {{Feature}}RestController {

    private final {{Feature}}Service {{feature}}Service;

    public {{Feature}}RestController({{Feature}}Service {{feature}}Service) {
        this.{{feature}}Service = {{feature}}Service;
    }

    @PostMapping("/insert")
    public void insert(@RequestBody {{Feature}}Form form) {
        {{feature}}Service.insert{{Feature}}Info(form);
    }

    @PostMapping("/update")
    public ResponseEntity<String> update(@RequestBody {{Feature}}Form form) {
        try {
            {{feature}}Service.update{{Feature}}Info(form);
            return ResponseEntity.ok("更新に成功しました");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新失敗");
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        try {
            {{feature}}Service.delete{{Feature}}InfoById(id);
            return ResponseEntity.ok("Deleted");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("削除に失敗しました");
        }
    }
}
```
