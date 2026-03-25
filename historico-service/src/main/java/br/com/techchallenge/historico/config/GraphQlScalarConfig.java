package br.com.techchallenge.historico.config;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class GraphQlScalarConfig {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Bean
    RuntimeWiringConfigurer runtimeWiringConfigurer() {
        GraphQLScalarType dateTimeScalar = GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("ISO-8601 LocalDateTime scalar")
                .coercing(new Coercing<LocalDateTime, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof LocalDateTime value) {
                            return FORMATTER.format(value);
                        }
                        throw new CoercingSerializeException("Valor invalido para DateTime");
                    }

                    @Override
                    public LocalDateTime parseValue(Object input) {
                        if (input instanceof String value) {
                            return parse(value);
                        }
                        throw new CoercingParseValueException("Valor invalido para DateTime");
                    }

                    @Override
                    public LocalDateTime parseLiteral(Object input) {
                        if (input instanceof StringValue value) {
                            return parse(value.getValue());
                        }
                        throw new CoercingParseLiteralException("Literal invalido para DateTime");
                    }

                    private LocalDateTime parse(String value) {
                        try {
                            return LocalDateTime.parse(value, FORMATTER);
                        } catch (Exception ex) {
                            throw new CoercingParseValueException("DateTime invalido", ex);
                        }
                    }
                })
                .build();

        return wiringBuilder -> wiringBuilder.scalar(dateTimeScalar);
    }
}
