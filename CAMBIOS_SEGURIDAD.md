# Cambios de seguridad — WebHardMon

## 1. Contraseña del administrador

La contraseña del admin inicial ya no está hardcodeada en el código. Ahora se lee de la variable de entorno `ADMIN_PASSWORD` definida en `.env`.

**Valor actual en `.env`:**
```
ADMIN_PASSWORD=WHM@Admin2024!
```

> **Cambia este valor antes de pasar a producción.**

**Requisitos mínimos de contraseña** (validados en el servidor):
- Mínimo 12 caracteres
- Al menos una mayúscula
- Al menos un número
- Al menos un carácter especial (`!@#$%...`)

### Si la base de datos ya existe (contraseña antigua "test")

El seeder solo crea datos cuando la BD está vacía. Si ya tienes datos, actualiza la contraseña manualmente con este comando SQL:

```sql
-- Genera el hash con Spring Security BCrypt strength 12
-- o usa el panel de admins una vez que puedas acceder con "test"
UPDATE administrador
SET password = '{bcrypt}HASH_AQUI'
WHERE username = 'admin';
```

O más rápido: accede con `admin` / `test`, entra en **Administratzaileak**, borra el admin actual y créalo de nuevo con la nueva contraseña. (No funciona porque no puedes borrar superadmins — ver sección 2.)

**La forma más sencilla si tienes acceso a MySQL:**
```sql
-- Primero, marca el admin como superAdmin si la columna ya existe
UPDATE administrador SET super_admin = 1 WHERE username = 'admin';
```
Luego usa el script de utilidad de abajo para generar el hash.

---

## 2. Sistema de roles

Se han añadido dos roles diferenciados:

| Rol | Descripción | Acceso |
|---|---|---|
| `ROLE_SUPERADMIN` | Solo la empresa propietaria | Panel completo + gestión de admins |
| `ROLE_ADMIN` | Admins de clientes | Solo su panel de empresa |

El campo `super_admin` (boolean) en la tabla `administrador` determina el rol. El admin inicial creado por el seeder tiene `super_admin = true`.

---

## 3. Gestión de administradores

**Ruta:** `/admin/usuarios` — solo accesible con `ROLE_SUPERADMIN`.

### Crear un admin nuevo
1. Accede con tu cuenta (superadmin)
2. Ve a **Administratzaileak** en el sidebar (solo visible para superadmins)
3. Rellena el formulario:
   - **Erabiltzaile-izena:** nombre de usuario del cliente
   - **Enpresa:** selecciona una empresa existente o escribe el nombre de una nueva
   - **Pasahitza / Berretsi:** contraseña que le darás al cliente
4. El cliente recibe sus credenciales directamente de ti — no hay registro público

### Borrar un admin
- Puedes borrar cualquier admin que NO sea superadmin
- No puedes borrar tu propia cuenta
- No puedes borrar cuentas superadmin

---

## 4. Cómo iniciar sesión

**URL:** `http://localhost:8080/login`

| Campo | Valor |
|---|---|
| Usuario | `admin` |
| Contraseña | El valor de `ADMIN_PASSWORD` en `.env` (`WHM@Admin2024!` por defecto) |

> Si la BD ya existía antes de estos cambios, la contraseña sigue siendo `test` hasta que la actualices.

---

## 5. Archivos modificados

| Archivo | Cambio |
|---|---|
| `.env` | Añadida variable `ADMIN_PASSWORD` |
| `application.yml` | Expuesta como `app.admin.password` |
| `model/Administrador.java` | Campo `superAdmin` |
| `security/AdminPrincipal.java` | Lógica de roles según `superAdmin` |
| `security/SecurityConfig.java` | Ruta `/admin/**` protegida con `ROLE_SUPERADMIN` |
| `config/DatabaseSeeder.java` | Hashea la contraseña desde env; marca admin como superAdmin |
| `controller/AdminController.java` | Nuevo — CRUD de administradores |
| `controller/WebController.java` | Pasa `isSuperAdmin` al modelo |
| `templates/admins.html` | Nueva — página de gestión de admins |
| `templates/layout.html` | Sidebar con link a admins (solo superadmin) |
| `templates/licencias.html` | Sidebar con link a admins (solo superadmin) |
| `schema-mysql.sql` | Columna `super_admin` en tabla `administrador` |
