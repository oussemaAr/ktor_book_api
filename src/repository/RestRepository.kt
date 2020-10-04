package elite.restapi.repository

import elite.restapi.db.DatabaseFactory.executeQuery
import elite.restapi.db.Mapper.rowToTodo
import elite.restapi.db.Mapper.rowToUser
import elite.restapi.model.Todo
import elite.restapi.model.User
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement

class RestRepository : Repository {
    override suspend fun addUser(email: String, displayName: String, passwordHash: String): User? {
        var statement: InsertStatement<Number>? = null
        executeQuery {
            statement = Users.insert { user ->
                user[Users.email] = email
                user[Users.displayName] = displayName
                user[Users.passwordHash] = passwordHash
            }
        }
        return rowToUser(statement?.resultedValues?.get(0))
    }

    override suspend fun deleteUser(userId: Int) {
        executeQuery {
            Users.deleteWhere {
                Users.userId.eq(userId)
            }
        }
    }

    override suspend fun findUser(userId: Int) = executeQuery {
        Users.select {
            Users.userId.eq(userId)
        }.map {
            rowToUser(it)
        }.singleOrNull()
    }

    override suspend fun findUserByEmail(email: String) = executeQuery {
        Users.select {
            Users.email.eq(email)
        }.map {
            rowToUser(it)
        }.firstOrNull()
    }

    override suspend fun addTodo(userId: Int, todo: String, done: Boolean): Todo? {
        var statement: InsertStatement<Number>? = null
        executeQuery {
            statement = Todos.insert {
                it[Todos.userId] = userId
                it[Todos.todo] = todo
                it[Todos.done] = done
            }
        }
        return rowToTodo(statement?.resultedValues?.get(0))
    }

    override suspend fun deleteTodo(userId: Int, todoId: Int) {
        executeQuery {
            Todos.deleteWhere {
                Todos.id.eq(todoId) and
                        Todos.userId.eq(userId)
            }
        }
    }

    override suspend fun findTodo(userId: Int, todoId: Int): Todo? {
        return executeQuery {
            Todos.select {
                Todos.id.eq(todoId) and
                        Todos.userId.eq((userId))
            }.map { rowToTodo(it) }.singleOrNull()
        }
    }

    override suspend fun getTodos(userId: Int): List<Todo> {
        return executeQuery {
            Todos.select {
                Todos.userId.eq((userId))
            }.mapNotNull { rowToTodo(it) }
        }
    }

    override suspend fun getTodos(userId: Int, offset: Int, limit: Int): List<Todo> {
        return executeQuery {
            Todos.select {
                Todos.userId.eq((userId))
            }.limit(limit, offset = offset.toLong()).mapNotNull { rowToTodo(it) }
        }
    }
}