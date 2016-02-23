# Scalist
### Todoist API client for Scala

[![Build Status](https://img.shields.io/travis/vpavkin/scalist/master.svg)](https://travis-ci.org/vpavkin/scalist) 
[![Coverage status](https://img.shields.io/codecov/c/github/vpavkin/scalist/master.svg)](https://codecov.io/github/vpavkin/scalist?branch=master)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/vpavkin/scalist/blob/master/LICENSE)
[![GitHub release](https://img.shields.io/github/release/vpavkin/scalist.svg)](https://github.com/vpavkin/scalist/releases)

Scalist is an API client library for Todoist, written in Scala. It is built on top of [Cats](https://github.com/typelevel/cats/) and [Shapeless](https://github.com/milessabin/shapeless) with a special attention to type safety. You don't have to be familiar with Shapeless or Cats to use this library.

Scalaist supports Scala 2.11 with Java 7/8.

**Warning:**
Project is at early stages now. Implemented feature set is not complete and there can be problems with perfomance. Also a great work should be done on clearing out what types should be made private/public.


1. [Quick start](#quick-start)
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

This is everything you need to start calling Todoist API. Here's some examples:

```scala
// get an authorized api wrapper
// here you'l need to have an implicit ExecutionContext in scope
val api = todoist.withToken("<your_token_here>")
```

```scala
// Load some resources. 
// Actual request execution starts only when you call execute on the request builder
api.get[Projects].and[Labels].execute
```

Not that any API call result has type `Future[Xor[Error, Result]]`, where:

- `Xor` is an alternative implementation of `Either` from Cats library (see more[http://typelevel.org/cats/tut/xor.html]). It's the only public dependency on Cats or Shapeless you'll have to use.
- `Error` can be either:
 - `HTTPError`: e.g. bad request
 - `DecodingError`: some inconsistency between Scalist and the Todoist API. If you got that, please file an issue [here](https://github.com/vpavkin/scalist/issues)
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

Should be mentioned here that Scalaist tracks all requested resources/commands and adjusts the result type accordingly at compile time. Consider these examples:
    result.filters - this won't compile, because we requested only Projects and Labels.
    // 
## Design
### Dependencies
 The only thing you'll need to directly work with is `cats.data.Xor`.
