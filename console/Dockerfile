FROM public.ecr.aws/lambda/java:11

COPY ./build/tmp/console/ /var/task/

# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "com.formkiq.stacks.console.ConsoleInstallHandler" ]