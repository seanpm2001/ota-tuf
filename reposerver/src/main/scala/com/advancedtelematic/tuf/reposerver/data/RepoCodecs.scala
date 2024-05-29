package com.advancedtelematic.tuf.reposerver.data

import com.advancedtelematic.tuf.reposerver.data.RepoDataType.{
  AddDelegationFromRemoteRequest,
  DelegationInfo,
  Package
}
import com.advancedtelematic.libtuf.data.ClientCodecs.*
import com.advancedtelematic.libats.codecs.CirceRefined.*
import com.advancedtelematic.libats.http.HttpCodecs.*
import com.advancedtelematic.libtuf.data.TufCodecs.*
import com.advancedtelematic.libats.codecs.CirceAts.*
import com.advancedtelematic.libats.http.HttpCodecs.*
import io.circe.Codec

object RepoCodecs {

  implicit val addDelegationFromRemoteRequestCodec: io.circe.Codec[AddDelegationFromRemoteRequest] =
    io.circe.generic.semiauto.deriveCodec[AddDelegationFromRemoteRequest]

  implicit val delegationInfoCodec: io.circe.Codec[DelegationInfo] =
    io.circe.generic.semiauto.deriveCodec[DelegationInfo]

  implicit val packageCodec: Codec[Package] =
    io.circe.generic.semiauto.deriveCodec[Package]

}
