package main

import "github.com/aws/aws-lambda-go/events"

type ArchiveDownloader struct {
}

func (downloader *ArchiveDownloader) DownloadArchiveAndDistributeDonwloadGameCommands(
	*events.APIGatewayV2HTTPRequest) (events.APIGatewayV2HTTPResponse, error) {
	panic("implement me")
}
