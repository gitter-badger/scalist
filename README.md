# Scalist
### Todoist API client for Scala

[![Build Status](https://img.shields.io/travis/vpavkin/scalist/master.svg)](https://travis-ci.org/vpavkin/scalist) 
[![Coverage status](https://img.shields.io/codecov/c/github/vpavkin/scalist/master.svg)](https://codecov.io/github/vpavkin/scalist?branch=master)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/vpavkin/scalist/blob/master/LICENSE)
[![GitHub release](https://img.shields.io/github/release/vpavkin/scalist.svg)](https://github.com/vpavkin/scalist/releases)

Scalist is an API client library for Todoist, written in Scala. It is built on top of [Cats](https://github.com/typelevel/cats/) and [Shapeless](https://github.com/milessabin/shapeless) with a special attention to type safety. You don't need deep knowledge of Shapeless or Cats to use this library.

Scalist supports Scala 2.11 with Java 7/8.

**Warning:**
Project is at early stages now. Implemented feature set is not complete and there can be problems with perfomance. Also, a great work should be done on clearing out what types should be made private/public.


1. [Quick start](#quick-start)
  1. [Importing](#importing)
  2. [Calling the API](#calling-the-api)
    1. [Queries](#queries)
    2. [Commands](#commands)
    3. [Request execution](#request-execution)
    4. [Response handling](#response-handling)
2. [Requirements](#requirements)
3. [Design](#design)
  1. [Dependencies](#dependencies)
  2. [Modules](#modules)
  3. [Type safety](#parsing)
  4. [Performance](#performance)
  5. [Documentation](#documentation)
  6. [Testing](#testing)
4. [Usage](#usage)
  1. [Encoding and decoding](#encoding-and-decoding)
  2. [Transforming JSON](#transforming-json)

## Quick start

### Importing
Currently, there's only one API implementation, based on [Dispatch HTTP](https://github.com/dispatch/reboot) and [Circe JSON](https://github.com/travisbrown/circe) libraries. To get it, include this in your `build.sbt`:

```scala
libraryDependencies += "ru.vpavkin" %% "scalist-dispatch-circe" % "0.1.0"
```

Next, import the API toolkit where you need it:

```scala
import ru.pavkin.todoist.api.core.model._
import ru.pavkin.todoist.api.dispatch.circe.default._
// Most of the times you would like to have syntax import alongside.
import ru.pavkin.todoist.api.dispatch.circe.default.syntax._
```

Use your token to get an authorized API wrapper:

```scala
// here you'l need to have an implicit ExecutionContext in scope
val api = todoist.withToken("<your_token_here>")
```

This is everything you need to start calling Todoist API.

### Calling the API

#### Queries

Build a single resource query:

```scala
val projectsReq = api.get[Projects]
```

Build a multiple resource query:

```scala
val multipleReq1 = api.get[Projects]
   .and[Labels]
   .and[Tasks]

// or this way (using shapeless HList)
val multipleReq2 = api.getAll[Projects :: Labels :: Tasks :: HNil]

// or this to load all resources
val multipleReq3 = api.getAll[All]
```

Scalist request builder tracks the list of requested resources, so that you can't, for instance, require same resource twice. These calls won't compile:

```scala
val invalidReq1 = api.get[Projects].and[Labels].and[Projects]
val invalidReq2 = api.getAll[Projects :: Projects :: HNil]
val invalidReq3 = api.getAll[All].and[Projects]
```

#### Commands

Build a single command request:

```scala
val addProject = api.perform(AddProject("Learn scalist", Some(ProjectColor.color18)))
```

Build a typesafe multiple command request (multiple ways):
```scala
import java.util.UUID

val existingProject: UUID = UUID.randomUUID() // and id of some existing project

// note the existingProject.projectId conversion: we have to tag the raw UUID as a ProjectId
val addStuff1 = api.perform(AddProject("Learn Scalist"))
                   .and(AddProject("Try Scalist"))
                   .and(AddTask("Add Scalist to my project", existingProject.projectId))

// or this way (using shapeless HList with some syntactic sugar to avoid HNils)
val addStuff2 = api.performAll(AddProject("Learn Scalist") :+
                               AddProject("Try Scalist") :+
                               AddTask("Add Scalist to my project", existingProject.projectId))
```

Command chains (with tempId):
```scala
// single dependant command
val addProjectWithTask = api.performAll(
    AddProject("Project").andForIt(projId => AddTask("Task1", projId)) 
    // or more concise: AddProject("Project").andForIt(AddTask("Task1", _)) 
)

// multiple dependant commands
val addProjectWithTasks = api.performAll(
    AddProject("Project").andForItAll(projId => 
      AddTask("Task1", projId) :+
      AddTask("Task2", projId) :+
      AddTask("Task3", projId)
    ) 
)
```

Note that resource ids, while being `UUID`s under the hood, are tagged with corresponding phantom types, so you won't be able to write things like this:
```scala
// labelId and taskId have differently tagged types
val invalidCommand = AddLabel("Label").andForIt(AddTask("Task1", _)) 
```

// Actual request execution starts only when you call execute on the request builder

Note that any API call result has type `Future[Xor[Error, Result]]`, where:

- `Xor` is an alternative implementation of `Either` from Cats library (see [more](http://typelevel.org/cats/tut/xor.html])). It's the only public dependency on Cats or Shapeless you'll have to use.
- `Error` can be either:
 - `HTTPError`: e.g. bad request.
 - `DecodingError`: this means some inconsistency between Scalist and the Todoist API. If you got that, please file an issue [here](https://github.com/vpavkin/scalist/issues).
- `Result` is type safe representation of API response, based on what you requested.

With this in mind, we can handle the result this way:

```scala
import cats.data.Xor // we need this for Xor pattern matching

api.get[Projects].and[Labels].execute.foreach{
  case Xor.Left(error) => println(s"Error: $error"
  case Xor.Right(result) => 
    println(s"Projects: ${result.projects})
    println(s"Labels: ${result.labels})
}
```

Should be mentioned here that Scalist tracks all requested resources/commands at compile time. It protects you from mistakes in building requests or. Consider these examples:
```scala
    result.filters - this won't compile, because we requested only Projects and Labels.
    // 
```
## Design
### Dependencies
 The only thing you'll need to directly work with is `cats.data.Xor`.
